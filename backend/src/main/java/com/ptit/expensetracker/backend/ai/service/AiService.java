package com.ptit.expensetracker.backend.ai.service;

import com.ptit.expensetracker.backend.ai.dto.ChatRequest;
import com.ptit.expensetracker.backend.ai.dto.ChatResponse;
import com.ptit.expensetracker.backend.ai.dto.InsightsRequest;
import com.ptit.expensetracker.backend.ai.dto.InsightsResponse;
import com.ptit.expensetracker.backend.ai.dto.ParseTransactionRequest;
import com.ptit.expensetracker.backend.ai.dto.ParsedTransactionDto;
import com.ptit.expensetracker.backend.ai.dto.ChatHistoryDto;
import com.ptit.expensetracker.backend.ai.dto.analyze.AiAnswerRequest;
import com.ptit.expensetracker.backend.ai.dto.analyze.AiAnswerResponse;
import com.ptit.expensetracker.backend.ai.dto.analyze.AiPlanRequest;
import com.ptit.expensetracker.backend.ai.dto.analyze.AiPlanResponse;

public interface AiService {

    ChatResponse chat(String userId, ChatRequest request);

    ParsedTransactionDto parseTransaction(String userId, ParseTransactionRequest request);

    InsightsResponse generateInsights(String userId, InsightsRequest request);

    AiPlanResponse plan(String userId, AiPlanRequest request);

    AiAnswerResponse answer(String userId, AiAnswerRequest request);

    java.util.List<ChatHistoryDto> getHistory(String userId);

    void clearHistory(String userId);
}



