package com.ptit.expensetracker.backend.ai.gemini;

import com.ptit.expensetracker.backend.ai.gemini.dto.GeminiRequest;
import com.ptit.expensetracker.backend.ai.gemini.dto.GeminiResponse;
import com.ptit.expensetracker.backend.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private final WebClient geminiWebClient;
    private final GeminiProperties properties;

    /**
     * Call Gemini chat endpoint with prepared prompt.
     */
    public String generateChatReply(String prompt, String userId, String locale) {
        return generateText(prompt);
    }

    /**
     * Generic text generation helper.
     */
    public String generateText(String prompt) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new ApiException("Gemini API key is not configured", "GEMINI_KEY_MISSING");
        }

        String path = String.format("/v1beta/models/%s:generateContent", properties.getModel());

        GeminiRequest request = GeminiRequest.builder()
                .contents(java.util.List.of(
                        GeminiRequest.Content.builder()
                                .parts(java.util.List.of(
                                        GeminiRequest.Part.builder().text(prompt).build()
                                ))
                                .build()
                ))
                .build();

        try {
            log.debug("Calling Gemini API: {} with model: {}", path, properties.getModel());
            
            GeminiResponse response = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("key", properties.getApiKey())
                            .build())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                        log.error("Gemini API returned status: {}", clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Gemini API error response: {}", body))
                                .then(Mono.error(new ApiException("Gemini API error: " + clientResponse.statusCode(), "GEMINI_API_ERROR")));
                    })
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofSeconds(20))
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(300)))
                    .onErrorResume(ex -> {
                        log.error("Gemini call failed", ex);
                        return Mono.error(new ApiException("Gemini call failed: " + ex.getMessage(), "GEMINI_CALL_FAILED"));
                    })
                    .block();

            if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                log.error("Gemini response is empty or has no candidates");
                throw new ApiException("Gemini response is empty", "GEMINI_EMPTY_RESPONSE");
            }

            return response.getCandidates().stream()
                    .map(GeminiResponse.Candidate::getContent)
                    .filter(c -> c != null && c.getParts() != null)
                    .flatMap(c -> c.getParts().stream())
                    .map(GeminiResponse.Part::getText)
                    .filter(t -> t != null && !t.isBlank())
                    .findFirst()
                    .orElseThrow(() -> new ApiException("Gemini returned no text", "GEMINI_NO_TEXT"));
                    
        } catch (Exception ex) {
            log.error("Unexpected error in Gemini call", ex);
            throw new ApiException("Gemini call failed: " + ex.getMessage(), "GEMINI_CALL_FAILED");
        }
    }

    /**
     * Call Gemini with function calling support.
     */
    public GeminiResponse callWithFunctions(GeminiRequest request) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new ApiException("Gemini API key is not configured", "GEMINI_KEY_MISSING");
        }

        String path = String.format("/v1beta/models/%s:generateContent", properties.getModel());

        try {
            log.debug("Calling Gemini API: {} with model: {}", path, properties.getModel());
            
            GeminiResponse response = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("key", properties.getApiKey())
                            .build())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> {
                        log.error("Gemini API returned status: {}", clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Gemini API error response: {}", body))
                                .then(Mono.error(new ApiException("Gemini API error: " + clientResponse.statusCode(), "GEMINI_API_ERROR")));
                    })
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofSeconds(20))
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(300)))
                    .onErrorResume(ex -> {
                        log.error("Gemini call with functions failed", ex);
                        return Mono.error(new ApiException("Gemini call failed: " + ex.getMessage(), "GEMINI_CALL_FAILED"));
                    })
                    .block();

            if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                log.error("Gemini response is empty or has no candidates");
                throw new ApiException("Gemini response is empty", "GEMINI_EMPTY_RESPONSE");
            }

            log.debug("Gemini call successful, got {} candidates", response.getCandidates().size());
            return response;
            
        } catch (Exception ex) {
            log.error("Unexpected error in Gemini call", ex);
            throw new ApiException("Gemini call failed: " + ex.getMessage(), "GEMINI_CALL_FAILED");
        }
    }
}


