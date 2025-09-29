package com.juleswhite.module1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AgentLoop {

    private static final int MAX_ITERATIONS = 10;
    private final List<Message> memory = new ArrayList<>();
    private final List<Message> agentRules = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentLoop() {
        // Initialize agent rules
        agentRules.add(new Message("system",
                """
                You are an AI agent that can perform tasks by using available tools.

                Available tools:
                - listFiles() -> List<String>: List all files in the current directory.
                - readFile(fileName: String) -> String: Read the content of a file.
                - terminate(message: String): End the agent loop and print a summary to the user.

                If a user asks about files, list them before reading.

                Every response MUST have an action.
                Respond in this format:

                ```action
                {
                    "toolName": "insert toolName",
                    "args": {...fill in any required arguments here...}
                }
                ```
                """));
    }

    public void run(String userRequest, LLM llm) throws IOException {
        int iterations = 0;

        memory.add(new Message("user", userRequest));

        while (iterations < MAX_ITERATIONS) {
            // Step 1: Construct prompt
            List<Message> prompt = new ArrayList<>(agentRules);
            prompt.addAll(memory);

            // Step 2: Generate response from LLM
            System.out.println("Agent thinking...");
            String response = llm.generateResponse(prompt);
            System.out.println("Agent response: " + response);

            // Step 3: Parse response to determine action
            Action action = parseAction(response);
            ActionResult result;

            // Step 4: Execute the appropriate action
            if ("listFiles".equals(action.getToolName())) {
                result = new ActionResult(listFiles(), null);
            } else if ("readFile".equals(action.getToolName())) {
                String fileName = (String) action.getArgs().get("fileName");
                result = new ActionResult(readFile(fileName), null);
            } else if ("terminate".equals(action.getToolName())) {
                System.out.println(action.getArgs().get("message"));
                break;
            } else if ("error".equals(action.getToolName())) {
                result = new ActionResult(null, (String) action.getArgs().get("message"));
            } else {
                result = new ActionResult(null, "Unknown action: " + action.getToolName());
            }

            System.out.println("Action result: " + result.toMap());

            // Step 5: Update memory
            memory.add(new Message("assistant", response));
            memory.add(new Message("user", objectMapper.writeValueAsString(result.toMap())));

            // Step 6: Check termination condition
            if ("terminate".equals(action.getToolName())) {
                break;
            }

            iterations++;
        }
    }

    private List<String> listFiles() {
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

    private String readFile(String fileName) {
        try {
            return Files.readString(new File(fileName).toPath());
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    private Action parseAction(String response) {
        try {
            String actionBlock = extractMarkdownBlock(response, "action");
            Action action = objectMapper.readValue(actionBlock, Action.class);
            if (action.getToolName() != null && action.getArgs() != null) {
                return action;
            } else {
                return errorAction("You must respond with a JSON tool invocation.");
            }
        } catch (Exception e) {
            return errorAction("Invalid JSON response. You must respond with a JSON tool invocation.");
        }
    }

    private String extractMarkdownBlock(String text, String label) {
        String startTag = "```" + label;
        String endTag = "```";
        int start = text.indexOf(startTag);
        int end = text.indexOf(endTag, startTag.length());
        if (start != -1 && end != -1) {
            return text.substring(start + startTag.length(), end).trim();
        }
        throw new IllegalArgumentException("No markdown block found for label: " + label);
    }

    private Action errorAction(String message) {
        Map<String, Object> args = new HashMap<>();
        args.put("message", message);
        return new Action("error", args);
    }
}