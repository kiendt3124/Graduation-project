package com.ptit.expensetracker.backend.ai.gemini;

import com.ptit.expensetracker.backend.ai.gemini.dto.GeminiResponse;
import com.ptit.expensetracker.backend.common.exception.ApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GeminiClientTest {

    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private GeminiClient buildClient(String apiKey) {
        GeminiProperties props = new GeminiProperties();
        props.setApiKey(apiKey);
        props.setBaseUrl(mockWebServer.url("/").toString());
        props.setModel("gemini-test");

        WebClient webClient = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();

        return new GeminiClient(webClient, props);
    }

    @Test
    void generateChatReply_success_returnsText() {
        String body = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {"text": "Hello from Gemini"}
                        ]
                      }
                    }
                  ]
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        GeminiClient client = buildClient("test-key");
        String reply = client.generateChatReply("prompt", "user", "vi-VN");

        assertEquals("Hello from Gemini", reply);
    }

    @Test
    void generateChatReply_noCandidates_throwsApiException() {
        String body = """
                { "candidates": [] }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        GeminiClient client = buildClient("test-key");
        assertThrows(ApiException.class, () -> client.generateChatReply("prompt", "user", "vi-VN"));
    }

    @Test
    void generateChatReply_missingApiKey_throwsApiException() {
        GeminiClient client = buildClient("");
        assertThrows(ApiException.class, () -> client.generateChatReply("prompt", "user", "vi-VN"));
    }
}


