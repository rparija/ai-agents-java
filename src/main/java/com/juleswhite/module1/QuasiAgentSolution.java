package com.juleswhite.module1;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuasiAgentSolution {

    private final List<Message> conversationHistory = new ArrayList<>();

    public QuasiAgentSolution() {
        // Initialize with system message
        conversationHistory.add(new Message("system",
                "You are an expert Java developer who specializes in writing clean, efficient, " +
                        "and well-documented code. When writing functions, you follow best practices " +
                        "and provide comprehensive documentation and test cases."));
    }

    public void run(LLM1 llm, String userFunctionRequest) {
        try {
            // Step 1: Generate basic function
            String basicFunction = generateBasicFunction(userFunctionRequest, llm);
            System.out.println("\nGenerated Basic Function:\n" + basicFunction);

            // Step 2: Add comprehensive documentation
            String documentedFunction = addDocumentation(basicFunction, llm);
            System.out.println("\nFunction with Documentation:\n" + documentedFunction);

            // Step 3: Add test cases
            String functionWithTests = addTestCases(documentedFunction, llm);
            System.out.println("\nFunction with Tests:\n" + functionWithTests);

            // Save to files
            saveToFile(basicFunction);
            saveToFile(documentedFunction);
            saveToFile(functionWithTests);

        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateBasicFunction(String userFunctionRequest, LLM1 llm) {
        // TODO: Implement this method
        // 1. Add user message to conversationHistory
        conversationHistory.add(new Message("user",
                "Write a basic Java function that does the following: " + userFunctionRequest +
                        "\nJust provide the function, no test cases or additional explanations yet."));        // 2. Send to LLM
        String response = llm.generateResponse(conversationHistory);
        // 3. Store LLM response in conversationHistory
        conversationHistory.add(new Message("assistant", response));
        // 4. Extract code from response
        String code = extractCodeFromResponse(response);
        // 5. Return the code
        if (!code.isEmpty()) {
            return code;
        }
        return "";
    }

    private String addDocumentation(String basicFunction, LLM1 llm) {
        // TODO: Implement this method
        // 1. Add user message asking for documentation to conversationHistory
        conversationHistory.add(new Message("user",
                "Add comprehensive JavaDoc documentation to the function you created. Include:\n" +
                        "1. Function description\n" +
                        "2. Parameter descriptions\n" +
                        "3. Return value description\n" +
                        "4. Example usage\n" +
                        "5. Edge cases\n\n" +
                        "Here's the function to document:\n\n" + basicFunction));
        // 2. Send to LLM
        String response=llm.generateResponse(conversationHistory);
        // 3. Store LLM response in conversationHistory
        conversationHistory.add(new Message("assistant",response));
        // 4. Extract documented code from response
        String code =extractCodeFromResponse(response);
        // 5. Return the documented code
        if(!code.isEmpty())
        {
            return code;
        }
        return "";
    }

    private String addTestCases(String documentedFunction, LLM1 llm) {
        // TODO: Implement this method
        // 1. Add user message asking for test cases to conversationHistory
        conversationHistory.add(new Message("user",
                "Create 3-4 JUnit test cases for the function. The tests should cover:\n" +
                        "1. Basic functionality\n" +
                        "2. Edge cases\n" +
                        "3. Error cases\n" +
                        "4. Various input scenarios\n\n" +
                        "Return just the test class. " +
                        "Here's the function to test:\n\n" + documentedFunction));
        // 2. Send to LLM
        String response=llm.generateResponse(conversationHistory);
        // 3. Store LLM response in conversationHistory
        conversationHistory.add(new Message("assistant",response));
        // 4. Extract code with tests from response
        String code =extractCodeFromResponse(response);
        // 5. Return the documented code
        if(!code.isEmpty())
        {
            return code;
        }
        return "";
    }

    private void saveToFile(String code) throws IOException {
        // Extract class name from the code to use as filename
        String className = extractClassName(code);
        String filename = className + ".java";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(code);
        }

        System.out.println("\nSaved to file: " + filename);
    }

    private String extractClassName(String code) {
        // Simple regex to find class name
        Pattern pattern = Pattern.compile("class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(code);

        if (matcher.find()) {
            return matcher.group(1);
        }

        // Default name if class name not found
        return "GeneratedFunction";
    }

    private String extractCodeFromResponse(String response) {
        // Try to extract code between ```java and ``` markers
        Pattern pattern = Pattern.compile("```java\\s*(.*?)\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // If no code block markers found, try to extract any Java-like code
        pattern = Pattern.compile("(public\\s+.*?\\{.*?\\})", Pattern.DOTALL);
        matcher = pattern.matcher(response);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // If nothing else works, return the full response
        System.out.println("Warning: Could not extract code from response, returning full text");
        return response;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // Create the LLM instance
            LLM1 llm = new LLM1();

            // Create the agent
            QuasiAgentSolution agent = new QuasiAgentSolution();

            // Get user input
            System.out.print("What Java function would you like me to create for you? Please describe what it should do: ");
            String userFunctionRequest = scanner.nextLine();

            // Run the agent
            agent.run(llm, userFunctionRequest);

        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}