package com.juleswhite.module2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.juleswhite.module2.Tool;
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LLM {
    private final String model;
    private final String apiKey;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private static final String HF_API_URL = "https://router.huggingface.co/v1/chat/completions";
    /**
     * Class to represent a prompt for the LLM, including messages and optional tools
     */
    public static class Prompt {
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
    public LLM() {
        this.model = "deepseek-ai/DeepSeek-V3.1-Terminus";//"openai/gpt-oss-120b";
        this.apiKey = System.getenv("HUGGINGFACE_API_KEY");
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public LLM(String model) {
        this.model = model;
        this.apiKey = System.getenv("HUGGINGFACE_API_KEY");
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String generateResponse(List<Message> messages) {
        return generateResponse(new Prompt(messages));
    }

    public String generateResponse(Prompt prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", this.model);
            List<Map<String, String>> modifiedMessages = new ArrayList<>();
            boolean toolsInjected = false;

            List<Map<String, String>> messageList = new ArrayList<>();
            for (Message msg : prompt.getMessages()) {
                Map<String, String> msgObj = new HashMap<>();
                msgObj.put("role", msg.getRole());
                msgObj.put("content", msg.getContent());
                messageList.add(msgObj);
            }
            requestBody.put("messages", messageList);
            requestBody.put("max_tokens", 1024);
            requestBody.put("temperature", 0.7);
            requestBody.put("top_p", 0.9);


          /*  Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", userPrompt);
            requestBody.put("parameters", Map.of(
                    "max_length", 150,
                    "temperature", 0.7,
                    "top_p", 0.9,
                    "return_full_text", false,
                    "do_sample", true
            ));*/

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(HF_API_URL)
                    .post(body)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response " + response);
                }

                String responseBody = response.body().string();
                List<Map<String, Object>> results = objectMapper.readValue(responseBody, List.class);
                return results.get(0).get("generated_text").toString();
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate response", e);
        }
    }

    // Call LLM API with context and get response
    public String callLLM(Prompt prompt) throws JsonProcessingException {

        // Prepare request payload (OpenAI format)
        Map<String, Object> payload = new HashMap<>();
        List<Map<String, String>> modifiedMessages = new ArrayList<>();
        boolean toolsInjected = false;
     /*   List<Map<String, String>> messages = new ArrayList<>();
        for (Message m : prompt.getMessages()) {
            Map<String, String> msg = new HashMap<>();
            msg.put("role", m.getRole());
            msg.put("content", m.getContent());
            messages.add(msg);
        }
        if (prompt.getTools() != null && !prompt.getTools().isEmpty()) {
            payload.put("tools", prompt.getTools());
        }
        payload.put("messages", messages);*/
        // --- Tool Injection Logic ---
        if (prompt.getTools() != null && !prompt.getTools().isEmpty()) {
            try {
                // 1. Convert the List<Tool> into a JSON array string.
                // The Tool class's properties (toolName, description, parameters)
                // must be what the LLM expects for function definitions.
                String toolDefinitionsJson = objectMapper.writeValueAsString(prompt.getTools());

                // 2. Construct the instruction/system message for the LLM
                String toolInjectionContent = "AVAILABLE TOOLS:\n" + toolDefinitionsJson + "\n\n"
                        + "INSTRUCTION: If a tool is required, you MUST respond ONLY with a JSON object following this exact format: "
                        + "`{\"tool\": \"<TOOL_NAME>\", \"args\": {<ARG_KEY>:<ARG_VALUE>, ...}}`. "
                        + "The toolName and parameters keys must match the definitions provided. Do not include any other text, explanation, or markdown formatting (like ```json). "
                        + "If no tool is needed, respond with a normal text message.\n\n"
                        + "--- Conversation Start ---";

                // 3. Prepend the instruction to the content of the first message.
                if (!prompt.getMessages().isEmpty()) {
                    Message firstMessage = prompt.getMessages().get(0);
                    Map<String, String> firstMsgMap = new HashMap<>();

                    // Prepend tool instructions to the content of the first message
                    firstMsgMap.put("role", firstMessage.getRole());
                    firstMsgMap.put("content", toolInjectionContent + "\n" + firstMessage.getContent());
                    modifiedMessages.add(firstMsgMap);

                    // Add the rest of the messages
                    for (int i = 1; i < prompt.getMessages().size(); i++) {
                        Message m = prompt.getMessages().get(i);
                        Map<String, String> msg = new HashMap<>();
                        msg.put("role", m.getRole());
                        msg.put("content", m.getContent());
                        modifiedMessages.add(msg);
                    }
                    toolsInjected = true;
                }

            } catch (JsonProcessingException e) {
                // Should not happen if Tool class is well-formed, but good to handle
                System.err.println("Error serializing tools: " + e.getMessage());
            }
        }

        // Fallback: If no tools or injection failed, use original messages
        if (!toolsInjected) {
            for (Message m : prompt.getMessages()) {
                Map<String, String> msg = new HashMap<>();
                msg.put("role", m.getRole());
                msg.put("content", m.getContent());
                modifiedMessages.add(msg);
            }
        }
        // --- End of Tool Injection Logic ---
        payload.put("messages", modifiedMessages);

        payload.put("model", model);

        // Prepare JSON payload
        String jsonPayload = objectMapper.writeValueAsString(payload);

        RequestBody requestBody = RequestBody.create(jsonPayload, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(HF_API_URL)
                .post(requestBody)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "LLM response failed: " + response.code();
            }
            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            Object choicesObj = responseMap.get("choices");
            if (choicesObj instanceof List && !((List<?>) choicesObj).isEmpty()) {
                Object messageObj = ((Map<?, ?>) ((List<?>) choicesObj).get(0)).get("message");
                if (messageObj instanceof Map) {
                    return (String) ((Map<?, ?>) messageObj).get("content");
                }
            }
            return "LLM response parsing failed.";
        } catch (Exception e) {
            return "LLM API error: " + e.getMessage();
        }
    }


    private String formatMessages(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message message : messages) {
            String role = message.getRole();
            String content = message.getContent();

            switch (role) {
                case "system":
                    sb.append("System: ").append(content).append("\n");
                    break;
                case "user":
                    sb.append("Human: ").append(content).append("\n");
                    break;
                case "assistant":
                    sb.append("Assistant: ").append(content).append("\n");
                    break;
            }
        }
        return sb.toString().trim();
    }
}