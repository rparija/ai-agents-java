package com.juleswhite.module2;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class LLMExampleWithFunctionCalling {

    public static void main(String[] args) {
        // Initialize the LLM
        LLM llm = new LLM();

        // Create a user message
        Message userMessage = new Message("user", "Please print a welcome message. Return the response in json format with a tool call to printMessage function.");

        // Define a Tool (function) called "printMessage" using JSON schema
        String schemaJson = """
        {
          "toolName": "printMessage",
          "description": "Prints a message to the console.",
          "parameters": {
            "type": "object",
            "properties": {
              "message": {
                "type": "string",
                "description": "The message to print."
              }
            },
            "required": ["message"]
          }
        }
        """;

        Tool printMessageTool = Tool.fromJson(schemaJson);

        // Build the Prompt
        LLM.Prompt prompt = new LLM.Prompt(
                Collections.singletonList(userMessage),
                Collections.singletonList(printMessageTool)
        );

        // Generate a response using the LLM with a function call
        String response = null;
        try {
            response = llm.callLLM(prompt);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // Print the raw response, which should have a tool call
        System.out.println("LLM Response: " + response);

        // Parse the response and figure out what tool the LLM wants to use
        try {

            // This will parse the JSON response from the LLM and
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> responseMap = mapper.readValue(response, Map.class);

            // Now, we can extract the tool name and arguments
            String toolName = (String) responseMap.get("tool");
            Map<String, Object> argsMap = (Map<String, Object>) responseMap.get("args");

            // Finally, we can call the tool with the arguments
            if ("printMessage".equals(toolName) && argsMap != null) {
                System.out.println("Using tool: " + toolName);
                String message = (String) argsMap.get("message");
                System.out.println("printMessage(" + message + ")");
            }
            else {
                System.out.println("No valid tool call found in the response.");
                System.out.println("LLM Tool: " + toolName);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse or execute tool call: " + e.getMessage());
            e.printStackTrace();
        }
    }
}