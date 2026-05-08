package com.ptit.expensetracker.backend.ai.gemini;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Duration;

@Configuration
public class GeminiWebClientConfig {

    @Bean
    public WebClient geminiWebClient(GeminiProperties properties) {
        // Wiretap can leak prompts/responses in logs; keep it OFF by default.
        boolean wiretapEnabled = Boolean.parseBoolean(System.getenv().getOrDefault("GEMINI_HTTP_WIRETAP", "false"));

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(20));

        if (wiretapEnabled) {
            httpClient = httpClient.wiretap(
                    "reactor.netty.http.client.HttpClient",
                    io.netty.handler.logging.LogLevel.INFO,
                    AdvancedByteBufFormat.TEXTUAL
            );
        }

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}


