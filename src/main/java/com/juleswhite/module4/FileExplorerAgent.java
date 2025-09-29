package com.juleswhite.module4;

import com.juleswhite.module4.*;
import java.util.List;

public class FileExplorerAgent {

    public static void main(String[] args) throws Exception {

        // Define the agent's goals
        List<Goal> goals = List.of(
                new Goal(1, "Navigate",
                        "Help the user accomplish tasks using the provided tools. " +
                                "Navigate the file system by changing directories and listing contents." +
                                "Avoid revisiting directories unless necessary."),
                new Goal(2, "Read",
                        "Read file contents when needed to answer user queries"),
                new Goal(3, "Search",
                        "Search for files or content based on user queries when appropriate"),
                new Goal(5, "Summarize",
                        "Provide clear summaries of findings in response to user requests"),
                new Goal(5, "Avoid Hidden Directories",
                        "Don't explore hidden directories unless explicitly requested"),
                new Goal(6, "Terminate",
                        "When the user's query has been answered, call terminate with a summary")
        );

        // Create an instance of FileExplorerTools
        FileExplorerTools explorerTools = new FileExplorerTools();

        // Create and run the agent with the instance
        Agent agent = Agents.createInstanceAgent(explorerTools, goals);

        // Default prompt to start exploring
        String initialPrompt = "Explore the directory and subdirectories. " +
                "When you have enough information, explain the architecture of the project.";

        if (args.length > 0) {
            // If an argument was provided, use it as the initial query
            initialPrompt = args[0];
        }

        Agents.runAndPrintResults(agent, initialPrompt, 55);
    }
}
