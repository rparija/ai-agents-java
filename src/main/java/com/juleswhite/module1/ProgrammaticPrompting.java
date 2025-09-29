package com.juleswhite.module1;

import java.util.ArrayList;
import java.util.List;

public class ProgrammaticPrompting {

    //
    // Set your API key as shown in the GitHub repository
    // export OPENAI_API_KEY="your-api-key"
    //
    public static void main(String[] args) {

        LLM1 llm = new LLM1();
        //LLMBase64 llm = new LLMBase64();

        // Create messages using the Message class
        List<Message> messages = new ArrayList<>();

        // Add system message
        messages.add(new Message("system",
                "You are an expert software engineer that prefers functional programming."));
        // Add user message
        messages.add(new Message("user",
                "Write a function to swap the keys and values in a map in java."));

        // Generate response using the LLM class
        String response = llm.generateResponse(messages);
        System.out.println(response);
    }
}