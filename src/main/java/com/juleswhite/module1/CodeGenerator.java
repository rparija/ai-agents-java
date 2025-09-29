package com.juleswhite.module1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CodeGenerator {

    public static void main(String[] args) throws Exception {
        LLM1 llm = new LLM1();

        // Create code specification
        Map<String, Object> codeSpec = new HashMap<>();
        codeSpec.put("name", "swap_keys_values");
        codeSpec.put("description", "Swaps the keys and values in a given dictionary.");

        Map<String, String> params = new HashMap<>();
        params.put("d", "A dictionary with unique values.");
        codeSpec.put("params", params);

        // Convert to JSON string
        ObjectMapper mapper = new ObjectMapper();
        String codeSpecJson = mapper.writeValueAsString(codeSpec);

        // Create messages using the Message class
        List<Message> messages = new ArrayList<>();

        // Add system message
        // Adjust this message if you want to focus on a specific programming language
        messages.add(new Message("system",
                "You are an expert software engineer that writes clean functional code. " +
                        "You always document your functions."));

        // Add user message
        messages.add(new Message("user",
                "Please implement: " + codeSpecJson));

        // Generate response using the LLM instance
        String response = llm.generateResponse(messages);
        System.out.println(response);
    }
}