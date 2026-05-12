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

import java.util.Base64;
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

    // ─── Main method: Text chat ─────────────────────────────────────────────

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

            return callGeminiApi(url, body, startMs);

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

    // ─── Vision method: Phân tích ảnh ────────────────────────────────────────

    /**
     * Gửi ảnh + text prompt tới Gemini Vision API.
     *
     * @param systemPrompt System instruction
     * @param textPrompt   Câu hỏi / yêu cầu về ảnh
     * @param imageBytes   Dữ liệu ảnh dạng byte[]
     * @param mimeType     Loại ảnh: "image/jpeg", "image/png", "image/webp"
     * @return GeminiResult chứa text response từ AI
     */
    public GeminiResult analyzeImage(String systemPrompt, String textPrompt,
                                      byte[] imageBytes, String mimeType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Gemini API key chưa được cấu hình.");
        }

        long startMs = System.currentTimeMillis();

        try {
            String url = apiUrl + "/models/" + model + ":generateContent?key=" + apiKey;

            ObjectNode body = objectMapper.createObjectNode();

            // System instruction
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            ObjectNode sysText = objectMapper.createObjectNode();
            sysText.put("text", systemPrompt);
            systemInstruction.set("parts", objectMapper.createArrayNode().add(sysText));
            body.set("systemInstruction", systemInstruction);

            // Contents: text + inline_data (ảnh base64)
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode userContent = objectMapper.createObjectNode();
            userContent.put("role", "user");

            ArrayNode parts = objectMapper.createArrayNode();

            // Part 1: text prompt
            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("text", textPrompt);
            parts.add(textPart);

            // Part 2: inline_data (ảnh base64)
            ObjectNode imagePart = objectMapper.createObjectNode();
            ObjectNode inlineData = objectMapper.createObjectNode();
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", Base64.getEncoder().encodeToString(imageBytes));
            imagePart.set("inline_data", inlineData);
            parts.add(imagePart);

            userContent.set("parts", parts);
            contents.add(userContent);
            body.set("contents", contents);

            // Generation config — temperature thấp cho output chính xác
            ObjectNode genConfig = objectMapper.createObjectNode();
            genConfig.put("maxOutputTokens", 1024);
            genConfig.put("temperature", 0.2);
            body.set("generationConfig", genConfig);

            return callGeminiApi(url, body, startMs);

        } catch (HttpClientErrorException e) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.error("[Gemini Vision] HTTP {} sau {}ms — body: {}", e.getStatusCode(), durationMs,
                    e.getResponseBodyAsString());
            throw new RuntimeException("Gemini Vision lỗi " + e.getStatusCode(), e);
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.error("[Gemini Vision] Lỗi sau {}ms: {}", durationMs, e.getMessage());
            throw new RuntimeException("Lỗi Gemini Vision: " + e.getMessage(), e);
        }
    }

    // ─── Shared: gọi API và parse response ──────────────────────────────────

    private GeminiResult callGeminiApi(String url, ObjectNode body, long startMs) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(
                objectMapper.writeValueAsString(body), headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, request, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());

        String text = root
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText("");

        // Kiểm tra finishReason
        String finishReason = root.path("candidates").path(0)
                .path("finishReason").asText("");
        if ("MAX_TOKENS".equals(finishReason)) {
            log.warn("[Gemini] Phản hồi bị cắt do đạt maxOutputTokens");
            text = text + "\n\n_(Phản hồi bị cắt do giới hạn độ dài.)_";
        }

        int promptTokens = root.path("usageMetadata")
                .path("promptTokenCount").asInt(0);
        int responseTokens = root.path("usageMetadata")
                .path("candidatesTokenCount").asInt(0);

        long durationMs = System.currentTimeMillis() - startMs;
        log.debug("[Gemini] OK — {}ms, prompt={} tokens, response={} tokens",
                durationMs, promptTokens, responseTokens);

        return new GeminiResult(text, promptTokens, responseTokens);
    }

    // ─── Message pair record ────────────────────────────────────────────────

    /**
     * Đại diện 1 tin nhắn trong history: role ("user" | "assistant") + content.
     */
    public record MessagePair(String role, String content) {
    }
}

