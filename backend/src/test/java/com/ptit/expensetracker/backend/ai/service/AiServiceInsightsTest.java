package com.ptit.expensetracker.backend.ai.service;

import com.ptit.expensetracker.backend.ai.dto.InsightsRequest;
import com.ptit.expensetracker.backend.ai.dto.InsightsResponse;
import com.ptit.expensetracker.backend.ai.gemini.GeminiClient;
import com.ptit.expensetracker.backend.ai.gemini.GeminiPromptBuilder;
import com.ptit.expensetracker.backend.ai.gemini.InsightsJsonParser;
import com.ptit.expensetracker.backend.ai.gemini.PlanJsonParser;
import com.ptit.expensetracker.backend.ai.gemini.TransactionJsonParser;
import com.ptit.expensetracker.backend.ai.gemini.AnswerJsonParser;
import com.ptit.expensetracker.backend.ai.repository.AiChatMessageRepository;
import com.ptit.expensetracker.backend.ai.service.impl.AiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiServiceInsightsTest {

    private GeminiClient geminiClient;
    private AiServiceImpl aiService;

    @BeforeEach
    void setup() {
        geminiClient = mock(GeminiClient.class);
        AiChatMessageRepository chatRepo = mock(AiChatMessageRepository.class);
        aiService = new AiServiceImpl(
                chatRepo,
                geminiClient,
                new GeminiPromptBuilder(),
                new TransactionJsonParser(),
                new InsightsJsonParser(),
                new PlanJsonParser(),
                new AnswerJsonParser(),
                mock(AiMemoryService.class)
        );
    }

    @Test
    void generateInsights_returnsParsedWhenGeminiOk() {
        String raw = """
                {"alerts":["A1"],"tips":["T1","T2"]}
                """;
        when(geminiClient.generateText(anyString())).thenReturn(raw);

        InsightsRequest req = new InsightsRequest();
        req.setTotalIncome(1000d);
        req.setTotalExpense(800d);
        req.setTotalDebt(0d);
        InsightsResponse resp = aiService.generateInsights("uid", req);

        assertEquals(List.of("A1"), resp.getAlerts());
        assertEquals(2, resp.getTips().size());
    }

    @Test
    void generateInsights_fallbackWhenGeminiFails() {
        when(geminiClient.generateText(anyString())).thenThrow(new RuntimeException("fail"));

        InsightsRequest req = new InsightsRequest();
        req.setTotalIncome(1000d);
        req.setTotalExpense(1200d);
        req.setTotalDebt(500d);
        InsightsResponse resp = aiService.generateInsights("uid", req);

        assertFalse(resp.getAlerts().isEmpty());
        assertFalse(resp.getTips().isEmpty());
    }
}


