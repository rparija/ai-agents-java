package com.juleswhite.module3;

import com.juleswhite.module3.LLM.Prompt;
import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

public class FileExplorerAgent {
    
    public static void main(String[] margs) throws Exception {
        // Define the agent's goals
        List<Goal> goals = List.of(
                new Goal(
                        1,
                        "Explore Files",
                        "Explore files in the current directory by listing and reading them"
                ),
                new Goal(
                        2,
                        "Terminate",
                        "Terminate the session when tasks are complete with a helpful summary"
                )
        );

        // Create the action registry
        ActionRegistry registry = new ActionRegistry();

        // Define and register the listFiles tool
        String listFilesJson = """
                {
                  "toolName": "listFiles",
                  "description": "List all files in the current directory",
                  "parameters": {
                    "type": "object",
                    "properties": {},
                    "required": []
                  },
                  "terminal": false
                }
                """;

        // Register the tool with its function
        Tool listFilesTool = Tool.fromJson(listFilesJson);
        registry.register(listFilesTool, (Function<Map<String,Object>,Object>) args -> {
            try {
                File dir = new File(".");
                return List.of(dir.list());
            } catch (Exception e) {
                return "Error listing files: " + e.getMessage();
            }
        });

        // Define and register the readFile tool
        String readFileJson = """
                {
                  "toolName": "readFile",
                  "description": "Read the contents of a specific file",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "fileName": {
                        "type": "string",
                        "description": "Name of the file to read"
                      }
                    },
                    "required": ["fileName"]
                  },
                  "terminal": false
                }
                """;

        // Register the tool with its function
        Tool readFileTool = Tool.fromJson(readFileJson);
        registry.register(readFileTool, (Function<Map<String,Object>,Object>) args -> {
            try {
                String fileName = (String) args.get("fileName");
                return new String(Files.readAllBytes(Paths.get(fileName)));
            } catch (java.nio.file.NoSuchFileException e) {
                return "Error: File '" + args.get("fileName") + "' not found.";
            } catch (Exception e) {
                return "Error reading file: " + e.getMessage();
            }
        });

        // Define and register the terminate tool
        String terminateJson = """
                {
                  "toolName": "terminate",
                  "description": "Terminate the conversation with a summary message",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "message": {
                        "type": "string",
                        "description": "Final summary message"
                      }
                    },
                    "required": ["message"]
                  },
                  "terminal": true
                }
                """;

        // Register the tool with its function
        Tool terminateTool = Tool.fromJson(terminateJson);
        registry.register(terminateTool, (Function<Map<String,Object>,Object>) args -> {
            String message = (String) args.get("message");
            return "Task completed: " + message;
        });

        // Create the environment
        Environment environment = new Environment(registry);

        // Create the agent language
        AgentLanguage agentLanguage = new AgentLanguages.FunctionCallingLanguage();

        // Create the LLM
        LLM llm = new LLM();

        // Define the LLM response generator
        Function<Prompt, String> generateResponse = prompt -> {
            return llm.generateResponse(prompt);
        };

        // Create the agent
        Agent fileExplorerAgent = new Agent(
                goals,
                registry,
                agentLanguage,
                environment,
                generateResponse
        );

        // Run the agent
        Scanner scanner = new Scanner(System.in);
        System.out.print("What would you like me to do? ");
        String userInput = scanner.nextLine();
        scanner.close();

        Memory memory = fileExplorerAgent.run(userInput, null, 10);

        // Print the final memory state
        System.out.println("\nFinal Memory State:");
        for (Map<String, Object> item : memory.getMemories()) {
            System.out.println(item.get("type") + ": " + item.get("content"));
        }
    }
}