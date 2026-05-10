package com.example.graduationproject.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * HTTP client gọi trực tiếp Google Gemini REST API.
 * Endpoint: POST /v1beta/models/{model}:generateContent?key={apiKey}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-flash-latest}")
    private String model;

    @Value("${gemini.api-url:https://generativelanguage.googleapis.com/v1beta}")
    private String apiUrl;

    @Value("${gemini.max-output-tokens:8192}")
    private int maxOutputTokens;

    private final ObjectMapper objectMapper;

    // ─── Result record ──────────────────────────────────────────────────────

    public record GeminiResult(String text, int promptTokens, int responseTokens) {
    }

    // ─── Main method ────────────────────────────────────────────────────────

    /**
     * Gửi conversation tới Gemini và nhận phản hồi.
     *
     * @param systemPrompt System instruction (context tài chính của user)
     * @param history      Lịch sử hội thoại [{role, content}, ...]
     * @return GeminiResult chứa text, promptTokens, responseTokens
     */
    public GeminiResult generateContent(String systemPrompt,
            List<MessagePair> history) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException(
                    "Gemini API key chưa được cấu hình. Vui lòng thêm GEMINI_API_KEY vào application.properties.");
        }

        long startMs = System.currentTimeMillis();

        try {
            String url = apiUrl + "/models/" + model + ":generateContent?key=" + apiKey;

            // ─── Build request body ──────────────────────────────────────
            ObjectNode body = objectMapper.createObjectNode();

            // System instruction
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            ObjectNode sysText = objectMapper.createObjectNode();
            sysText.put("text", systemPrompt);
            systemInstruction.set("parts", objectMapper.createArrayNode().add(sysText));
            body.set("systemInstruction", systemInstruction);

            // Conversation contents
            ArrayNode contents = objectMapper.createArrayNode();
            for (MessagePair msg : history) {
                ObjectNode content = objectMapper.createObjectNode();
                // Gemini dùng "model" thay vì "assistant"
                content.put("role", "assistant".equals(msg.role()) ? "model" : msg.role());
                ArrayNode parts = objectMapper.createArrayNode();
                ObjectNode part = objectMapper.createObjectNode();
                part.put("text", msg.content());
                parts.add(part);
                content.set("parts", parts);
                contents.add(content);
            }
            body.set("contents", contents);

            // Generation config
            ObjectNode genConfig = objectMapper.createObjectNode();
            genConfig.put("maxOutputTokens", maxOutputTokens);
            genConfig.put("temperature", 0.7);
            body.set("generationConfig", genConfig);

            // ─── HTTP call ──────────────────────────────────────────────
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body), headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);

            // ─── Parse response ─────────────────────────────────────────
            JsonNode root = objectMapper.readTree(response.getBody());

            String text = root
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("");

            // ─── Kiểm tra finishReason ──────────────────────────────────
            String finishReason = root.path("candidates").path(0)
                    .path("finishReason").asText("");
            if ("MAX_TOKENS".equals(finishReason)) {
                log.warn("[Gemini] Phản hồi bị cắt do đạt maxOutputTokens={}", maxOutputTokens);
                text = text + "\n\n_(Phản hồi bị cắt do giới hạn độ dài. Bạn có thể hỏi tiếp để xem thêm.)_";
            }

            int promptTokens = root.path("usageMetadata")
                    .path("promptTokenCount").asInt(0);
            int responseTokens = root.path("usageMetadata")
                    .path("candidatesTokenCount").asInt(0);

            long durationMs = System.currentTimeMillis() - startMs;
            log.debug("[Gemini] OK — {}ms, prompt={} tokens, response={} tokens",
                    durationMs, promptTokens, responseTokens);

            return new GeminiResult(text, promptTokens, responseTokens);

        } catch (HttpClientErrorException e) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.error("[Gemini] HTTP {} sau {}ms — body: {}", e.getStatusCode(), durationMs,
                    e.getResponseBodyAsString());
            throw new RuntimeException("Gemini API lỗi " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.error("[Gemini] Lỗi sau {}ms: {}", durationMs, e.getMessage());
            throw new RuntimeException("Lỗi kết nối Gemini AI: " + e.getMessage(), e);
        }
    }

    // ─── Message pair record ────────────────────────────────────────────────

    /**
     * Đại diện 1 tin nhắn trong history: role ("user" | "assistant") + content.
     */
    public record MessagePair(String role, String content) {
    }
}
