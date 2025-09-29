package com.juleswhite.module2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

public class AgentLoopFunctionCalling {

    private final Map<String, Function<Map<String, Object>, Object>> toolFunctions = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Tool> tools;
    private final int maxIterations;

    public AgentLoopFunctionCalling(int maxIterations) {
        this.tools = new ArrayList<>();
        this.maxIterations = maxIterations;
    }

    /**
     * Register a tool with the agent.
     *
     * @param toolJson JSON defining the tool
     * @param function The function to execute when the tool is called
     */
    public void registerTool(String toolJson, Function<Map<String, Object>, Object> function) {
        Tool tool = Tool.fromJson(toolJson);
        tools.add(tool);
        toolFunctions.put(tool.getToolName(), function);
    }

    /**
     * Parse the LLM response into an Action object.
     *
     * @param response The LLM response string
     * @return The parsed Action object
     * @throws Exception If the response cannot be parsed
     */
    private Action parseAction(String response) throws Exception {
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        String toolName = (String) responseMap.get("tool");
        Map<String, Object> toolArgs = (Map<String, Object>) responseMap.get("args");
        return new Action(toolName, toolArgs);
    }

    /**
     * Execute a single action and return the result.
     *
     * @param action The action to execute
     * @return The result of the action
     */
    private ActionResult executeAction(Action action) {
        System.out.println("Executing: " + action.getToolName() + " with args " + action.getArgs());

        try {
            if (toolFunctions.containsKey(action.getToolName())) {
                Object result = toolFunctions.get(action.getToolName()).apply(action.getArgs());
                return new ActionResult(result, null);
            } else {
                return new ActionResult(null, "Unknown tool: " + action.getToolName());
            }
        } catch (Exception e) {
            return new ActionResult(null,
                    "Error executing " + action.getToolName() + ": " + e.getMessage());
        }
    }

    /**
     * Run the agent loop with the given user request.
     *
     * @param userRequest The initial user request.
     * @param llm The LLM instance to use.
     * @return The final state of the memory list.
     */
    public List<Message> run(String userRequest, LLM llm) {
        // Initialize memory with system message and user request
        List<Message> memory = new ArrayList<>();
        String systemPrompt = """
                You are an AI agent that can perform tasks by requesting available tools.
                Available tools:
                - listFiles: Returns a list of files in the current directory.
                - readFile: Reads the content of a specified file.
                - terminate: Ends the conversation with a final message.

                When you need to perform an action, respond with a JSON object specifying the tool and its arguments, for example:
                { "tool": "listFiles", "args": {} }

                If you need to read a file, first use "listFiles" to see available files, then use "readFile" with the file name.
                When finished, use "terminate" with a message for the user.
                """;
        memory.add(new Message("system", systemPrompt));
        memory.add(new Message("user", userRequest));

        int iterations = 0;

        // The Agent Loop
        while (iterations < maxIterations) {
            // Create LLM prompt with current memory and tools
            LLM.Prompt prompt = new LLM.Prompt(memory, tools);

            // Get LLM response
            String response = null;
            try {
                response = llm.callLLM(prompt);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            try {
                // Parse the response to extract the action
                Action action = parseAction(response);

                if ("terminate".equals(action.getToolName())) {
                    System.out.println("Termination message: " + action.getArgs().get("message"));
                    memory.add(new Message("assistant", response));
                    break;
                } else {
                    // Execute the action and get result
                    ActionResult result = executeAction(action);
                    System.out.println("Result: " + result.toMap());

                    // Update memory
                    memory.add(new Message("assistant", response));
                    memory.add(new Message("user", objectMapper.writeValueAsString(result.toMap())));
                }
            } catch (Exception e) {
                // Handle standard text response or parsing error
                System.out.println("Response: " + response);
                memory.add(new Message("assistant", response));
                break;
            }

            iterations++;
        }

        return memory;
    }

    public static void main(String[] args) {
        try {
            // Get user input
            System.out.print("What would you like me to do? ");
            Scanner scanner = new Scanner(System.in);
            String userTask = scanner.nextLine();
            scanner.close();

            // Define tools using our Tool class with JSON
            String listFilesJson = """
            {
              "toolName": "listFiles",
              "description": "Returns a list of files in the directory.",
              "parameters": {
                "type": "object",
                "properties": {},
                "required": []
              }
            }
            """;

            String readFileJson = """
            {
              "toolName": "readFile",
              "description": "Reads the content of a specified file in the directory.",
              "parameters": {
                "type": "object",
                "properties": {
                  "fileName": { 
                    "type": "string",
                    "description": "The name of the file to read"
                  }
                },
                "required": ["fileName"]
              }
            }
            """;

            String terminateJson = """
            {
              "toolName": "terminate",
              "description": "Terminates the conversation. No further actions or interactions are possible after this. Prints the provided message for the user.",
              "parameters": {
                "type": "object",
                "properties": {
                  "message": { 
                    "type": "string",
                    "description": "The final message to display to the user"
                  }
                },
                "required": ["message"]
              }
            }
            """;

            // Create agent instance
            AgentLoopFunctionCalling agent = new AgentLoopFunctionCalling(10);

            // Register tools
            agent.registerTool(listFilesJson, targs -> listFiles());
            agent.registerTool(readFileJson, targs -> readFile((String) targs.get("fileName")));
            agent.registerTool(terminateJson, targs -> {
                System.out.println("Termination message: " + targs.get("message"));
                return "Agent terminated";
            });

            // Initialize the LLM
            LLM llm = new LLM();

            // Run the agent
            List<Message> finalMemory = agent.run(userTask, llm);

            System.out.println("Agent completed with " + finalMemory.size() + " memory entries.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Tool implementation methods...
    private static List<String> listFiles() {
        File currentDir = new File(".");
        File[] files = currentDir.listFiles();
        List<String> fileNames = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileNames.add(file.getName());
                }
            }
        }
        return fileNames;
    }

    private static String readFile(String fileName) {
        try {
            return Files.readString(new File(fileName).toPath());
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }
}