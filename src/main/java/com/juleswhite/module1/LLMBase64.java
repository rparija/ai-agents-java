package com.juleswhite.module1;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Base64;

public class LLMBase64 {
    private final String model;
    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public LLMBase64() {
        String envModel = System.getenv("HF_MODEL_ID");
        this.model = (envModel != null && !envModel.isBlank()) ? envModel : "openai/gpt-oss-20b:novita";

        String key = System.getenv("HUGGINGFACE_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getenv("HF_TOKEN");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Missing API key");
        }
        this.apiKey = key;

        this.baseUrl = "https://router.huggingface.co/v1/chat/completions";

        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();

        this.objectMapper = new ObjectMapper();
    }

    public String generateResponse(List<Message> messages) {
        try {
            List<Map<String, String>> formattedMessages = new ArrayList<>();
            for (Message msg : messages) {
                Map<String, String> messageMap = new HashMap<>();
                messageMap.put("role", msg.getRole());
                messageMap.put("content", msg.getContent());
                formattedMessages.add(messageMap);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", formattedMessages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 512);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonBody);

            Request request = new Request.Builder()
                    .url(baseUrl)
                    .post(body)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    throw new IOException("Unexpected response: " + response.code() + " - " + errorBody);
                }

                String responseBody = response.body().string();
                Map<String, Object> jsonResponse = objectMapper.readValue(responseBody, Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) jsonResponse.get("choices");

                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                    if (message != null) {
                        String content = message.get("content");
                        return Base64.getEncoder().encodeToString(content.getBytes());
                    }
                }
                throw new IOException("Invalid response structure");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate response: " + e.getMessage(), e);
        }
    }
}