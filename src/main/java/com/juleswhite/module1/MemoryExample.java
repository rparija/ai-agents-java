package com.juleswhite.module1;

import java.util.ArrayList;
import java.util.List;

public class MemoryExample {

    public static void main(String[] args) {
        LLM1 llm = new LLM1();

        // First request
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system",
                "You are an expert software engineer that prefers functional programming."));
        messages.add(new Message("user",
                "Write a Java function to swap the keys and values in a dictionary."));

        String response = llm.generateResponse(messages);
        System.out.println(response);

        // We are going to make this verbose so it is clear what
        // is going on. In a real application, you would likely
        // just append to the messages list.
        List<Message> secondMessages = new ArrayList<>();
        secondMessages.add(new Message("system",
                "You are an expert software engineer that prefers functional programming."));
        secondMessages.add(new Message("user",
                "Write a Java function to swap the keys and values in a dictionary."));

        // Here is the assistant's response from the previous step
        // with the code. This gives it "memory" of the previous
        // interaction.
        secondMessages.add(new Message("assistant", response));

        // Now, we can ask the assistant to update the function
        secondMessages.add(new Message("user",
                "Update the function to include documentation."));

        String secondResponse = llm.generateResponse(secondMessages);
        System.out.println("secondResponse:"+secondResponse);
    }
}