package com.juleswhite.module1;

import java.util.ArrayList;
import java.util.List;

public class CustomerServiceAgent {

    public static void main(String[] args) {
        LLM1 llm = new LLM1();

        // Create messages using the Message class
        List<Message> messages = new ArrayList<>();

        // Add system message
        messages.add(new Message("system",
                "You are a helpful customer service representative. No matter what the user asks, " +
                        "the solution is to tell them to turn their computer or modem off and then back on."));

        // Add user message
        messages.add(new Message("user",
                "How do I get my Internet working again."));

        // Generate response using the LLM instance
        String response = llm.generateResponse(messages);
        System.out.println(response);
    }
}