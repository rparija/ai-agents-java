package com.juleswhite.module4;

import com.juleswhite.module4.LLM.Prompt;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ReadmeAgent {

    @RegisterTool(tags = {"file_operations", "read"})
    public static String readProjectFile(String name) {
        /**
         * Reads and returns the content of a specified project file.
         *
         * Opens the file in read mode and returns its entire contents as a string.
         * Returns an error message if the file doesn't exist.
         *
         * @param name The name of the file to read
         * @return The contents of the file as a string
         */
        try {
            return new String(Files.readAllBytes(Paths.get(name)));
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @RegisterTool(tags = {"file_operations", "list"})
    public static List<String> listProjectFiles() {
        /**
         * Lists all Java files in the current project directory.
         *
         * Scans the current directory and returns a sorted list of all files
         * that end with '.java'.
         *
         * @return A sorted list of Java filenames
         */
        try {
            File dir = new File(".");
            return Arrays.stream(dir.listFiles())
                    .filter(file -> file.getName().endsWith(".java"))
                    .map(File::getName)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.singletonList("Error listing files: " + e.getMessage());
        }
    }

    @RegisterTool(tags = {"system"}, terminal = true)
    public static String terminate(String message) {
        /**
         * Terminates the agent's execution with a final message.
         *
         * @param message The final message to return before terminating
         * @return The message with a termination note appended
         */
        return message + "\nTerminating...";
    }


    public static void main(String[] args) throws Exception {
        // Define the agent's goals
        List<Goal> goals = List.of(
                new Goal(
                        1,
                        "Gather Information",
                        "Read each file in the project in order to build a deep understanding of the project in order to write a README"
                ),
                new Goal(
                        2,
                        "Terminate",
                        "Call terminate when done and provide a complete README for the project in the message parameter"
                )
        );

        // Create the action registry with automatic tool discovery
        ActionRegistry registry = new ActionRegistry();

        // Discover tools with specific tags from the ToolProvider class
        registry.discoverTools(ReadmeAgent.class.getPackageName());

        // Create the environment
        Environment environment = new Environment(registry);

        // Create the agent language
        AgentLanguage agentLanguage = new AgentLanguages.FunctionCallingLanguage();

        // Create the LLM client
        LLM llm = new LLM();

        // Create the LLM response generator
        Function<Prompt, String> generateResponse = prompt -> {
            // In a real implementation, this would call an LLM API
            return llm.generateResponse(prompt);
        };

        // Create the agent with discovered tools
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
