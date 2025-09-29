package com.juleswhite.module3;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReadmeAgent {

    /**
     *
     * This example will read all of the Java files in the current directory and
     * print the content of a README file explaining them to the console.
     *
     * @param margs
     * @throws Exception
     */
    public static void main(String[] margs) throws Exception {
        // Define the agent's goals
        List<Goal> goals = List.of(
                new Goal(
                        1,
                        "Gather Information",
                        "Read each file in the project"
                ),
                new Goal(
                        1,
                        "Terminate",
                        "Call the terminate tool when you have read all the files " +
                                "and provide the content of the README in the terminate message"
                )
        );

        // Create the action registry
        ActionRegistry registry = new ActionRegistry();

        // Define and register the listProjectFiles tool
        String listProjectFilesJson = """
        {
          "toolName": "listProjectFiles",
          "description": "Lists all Java files in the project.",
          "parameters": {
            "type": "object",
            "properties": {},
            "required": []
          },
          "terminal": false
        }
        """;

        // Register the tool with its function
        Tool listProjectFilesTool = Tool.fromJson(listProjectFilesJson);
        registry.register(listProjectFilesTool, (Function<Map<String,Object>,Object>) args -> {
            try {
                File dir = new File(".");
                return Arrays.stream(dir.listFiles())
                        .filter(file -> file.getName().endsWith(".java"))
                        .map(File::getName)
                        .sorted()
                        .collect(Collectors.toList());
            } catch (Exception e) {
                return "Error listing files: " + e.getMessage();
            }
        });

        // Define and register the readProjectFile tool
        String readProjectFileJson = """
        {
          "toolName": "readProjectFile",
          "description": "Reads a file from the project.",
          "parameters": {
            "type": "object",
            "properties": {
              "name": {
                "type": "string",
                "description": "Name of the file to read"
              }
            },
            "required": ["name"]
          },
          "terminal": false
        }
        """;

        // Register the tool with its function
        Tool readProjectFileTool = Tool.fromJson(readProjectFileJson);
        registry.register(readProjectFileTool, (Function<Map<String,Object>,Object>) args -> {
            try {
                String fileName = (String) args.get("name");
                return new String(Files.readAllBytes(Paths.get(fileName)));
            } catch (java.nio.file.NoSuchFileException e) {
                return "Error: File '" + args.get("name") + "' not found.";
            } catch (Exception e) {
                return "Error reading file: " + e.getMessage();
            }
        });

        // Define and register the terminate tool
        String terminateJson = """
        {
          "toolName": "terminate",
          "description": "Terminates the session and prints the message to the user.",
          "parameters": {
            "type": "object",
            "properties": {
              "message": {
                "type": "string",
                "description": "Final message containing the README content"
              }
            },
            "required": ["message"]
          },
          "terminal": true
        }
        """;

        // Register the tool with its function
        Tool terminateTool = Tool.fromJson(terminateJson);
        registry.register(terminateTool, (Function<Map<String,Object>,Object>)args -> {
            String message = (String) args.get("message");
            return message + "\nTerminating...";
        });

        // Create the environment
        Environment environment = new Environment(registry);

        // Create the agent language
        AgentLanguage agentLanguage = new AgentLanguages.FunctionCallingLanguage();

        // Create the LLM
        LLM llm = new LLM();

        // Create the response generator function that uses our LLM
        Function<LLM.Prompt, String> generateResponse = prompt -> {
            return llm.generateResponse(prompt);
        };

        // Create the agent
        Agent readmeAgent = new Agent(
                goals,
                registry,
                agentLanguage,
                environment,
                generateResponse
        );

        // Run the agent with our task
        String userInput = "Write a README for this project.";
        Memory finalMemory = readmeAgent.run(userInput, null, 10);

        // Print the final memory state
        System.out.println("\nFinal Memory State:");
        for (Map<String, Object> item : finalMemory.getMemories()) {
            System.out.println(item.get("type") + ": " + item.get("content"));
        }
    }
}
