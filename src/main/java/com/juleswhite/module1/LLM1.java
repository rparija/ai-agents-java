package com.juleswhite.module1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.juleswhite.module2.Tool;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LLM1 {
    private final String model;
    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public LLM1() {
        String envModel = System.getenv("HF_MODEL_ID");
        this.model = (envModel != null && !envModel.isBlank()) ? envModel : "openai/gpt-oss-20b:novita";

        String key = System.getenv("HUGGINGFACE_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getenv("HF_TOKEN");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Missing Hugging Face API key. Set HUGGINGFACE_API_KEY or HF_TOKEN.");
        }
        this.apiKey = key;

        String envBase = System.getenv("HF_BASE_URL");
        this.baseUrl = (envBase != null && !envBase.isBlank()) ? envBase : "https://router.huggingface.co/v1/chat/completions";

        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String generateResponse(List<Message> messages) {
        try {
            List<Map<String, String>> chatMessages = new java.util.ArrayList<>();
            for (Message msg : messages) {
                Map<String, String> entry = new java.util.HashMap<>();
                entry.put("role", msg.getRole());
                entry.put("content", msg.getContent());
                chatMessages.add(entry);
            }

            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", this.model);
            requestBody.put("messages", chatMessages);
            requestBody.put("max_tokens", 1024);
            requestBody.put("temperature", 0.3);
            requestBody.put("top_p", 0.9);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(this.baseUrl)
                    .post(body)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                int status = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response: HTTP " + status + " - " + responseBody);
                }
                Map<String, Object> root = objectMapper.readValue(responseBody, java.util.Map.class);
                Object choicesObj = root.get("choices");
                if (!(choicesObj instanceof java.util.List<?>)) {
                    throw new IOException("Unexpected response structure: missing 'choices'.");
                }
                java.util.List<?> choices = (java.util.List<?>) choicesObj;
                if (choices.isEmpty()) {
                    throw new IOException("No choices returned from inference API.");
                }
                Object firstChoice = choices.get(0);
                if (!(firstChoice instanceof java.util.Map<?, ?>)) {
                    throw new IOException("Unexpected response structure: choice is not an object.");
                }
                java.util.Map<?, ?> choiceMap = (java.util.Map<?, ?>) firstChoice;
                Object messageObj = choiceMap.get("message");
                if (!(messageObj instanceof java.util.Map<?, ?>)) {
                    throw new IOException("Unexpected response structure: missing 'message'.");
                }
                java.util.Map<?, ?> messageMap = (java.util.Map<?, ?>) messageObj;
                Object content = messageMap.get("content");
                return content != null ? content.toString().trim() : "";
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate response: " + e.getMessage(), e);
        }
    }


    public static void main(String[] args) {
        LLM1 llm = new LLM1();
        List<Message> messages = List.of(
                new Message("user", "Tell me a joke about cats.")
        );

        String response = llm.generateResponse(messages);
        System.out.println("Generated response:\n" + response);
    }

    /**
     * Class to represent a prompt for the LLM, including messages and optional tools
     */
    public static class Prompt
    {
        private List<com.juleswhite.module2.Message> messages;
        private List<com.juleswhite.module2.Tool> tools;
        private Map<String, Object> metadata;


        public Prompt(List<com.juleswhite.module2.Message> messages) {
        this.messages = messages;
        this.tools = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

        public Prompt(List<com.juleswhite.module2.Message> messages, List<com.juleswhite.module2.Tool> tools) {
        this.messages = messages;
        this.tools = tools != null ? tools : new ArrayList<>();
        this.metadata = new HashMap<>();
    }

        public Prompt(List<com.juleswhite.module2.Message> messages, List<com.juleswhite.module2.Tool> tools, Map<String, Object> metadata) {
        this.messages = messages;
        this.tools = tools != null ? tools : new ArrayList<>();
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

        public List<com.juleswhite.module2.Message> getMessages() {
        return messages;
    }

        public List<Tool> getTools() {
        return tools;
    }

        public Map<String, Object> getMetadata() {
        return metadata;
    }
    }
}
