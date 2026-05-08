package com.ptit.expensetracker.backend.ai.service.impl;

import com.ptit.expensetracker.backend.ai.domain.AiChatMessage;
import com.ptit.expensetracker.backend.ai.domain.MessageRole;
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
import com.ptit.expensetracker.backend.ai.dto.analyze.DataRequest;
import com.ptit.expensetracker.backend.ai.dto.analyze.DatasetType;
import com.ptit.expensetracker.backend.ai.gemini.GeminiClient;
import com.ptit.expensetracker.backend.ai.gemini.GeminiPromptBuilder;
import com.ptit.expensetracker.backend.ai.gemini.TransactionJsonParser;
import com.ptit.expensetracker.backend.ai.gemini.InsightsJsonParser;
import com.ptit.expensetracker.backend.ai.gemini.PlanJsonParser;
import com.ptit.expensetracker.backend.ai.gemini.AnswerJsonParser;
import com.ptit.expensetracker.backend.ai.repository.AiChatMessageRepository;
import com.ptit.expensetracker.backend.ai.service.AiService;
import com.ptit.expensetracker.backend.ai.service.AiMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiChatMessageRepository chatMessageRepository;
    private final GeminiClient geminiClient;
    private final GeminiPromptBuilder promptBuilder;
    private final TransactionJsonParser transactionJsonParser;
    private final InsightsJsonParser insightsJsonParser;
    private final PlanJsonParser planJsonParser;
    private final AnswerJsonParser answerJsonParser;
    private final AiMemoryService memoryService;

    @Override
    public ChatResponse chat(String userId, ChatRequest request) {
        AiChatMessage userMessage = AiChatMessage.builder()
                .userId(userId)
                .role(MessageRole.USER)
                .content(request.getMessage())
                .createdAt(OffsetDateTime.now())
                .build();
        chatMessageRepository.save(userMessage);

        String contextHeader = memoryService.buildContextHeader(userId);
        String recent = memoryService.buildRecentVisibleTranscript(userId);
        String prompt = promptBuilder.buildChatPrompt(
                (contextHeader.isBlank() ? "" : contextHeader + "\n\n")
                        + (recent.isBlank() ? "" : ("Recent messages:\n" + recent + "\n\n"))
                        + "User message: " + request.getMessage(),
                userId,
                request.getLocale()
        );

        String reply = geminiClient.generateChatReply(prompt, userId, request.getLocale());

        AiChatMessage assistantMessage = AiChatMessage.builder()
                .userId(userId)
                .role(MessageRole.ASSISTANT)
                .content(reply)
                .createdAt(OffsetDateTime.now())
                .build();
        chatMessageRepository.save(assistantMessage);
        memoryService.updateMemoryIfNeeded(userId);

        return ChatResponse.builder()
                .reply(reply)
                .suggestions(Collections.emptyList())
                .build();
    }

    @Override
    public ParsedTransactionDto parseTransaction(String userId, ParseTransactionRequest request) {
        String prompt = promptBuilder.buildParseTransactionPrompt(request.getText(), request.getLocale());
        String raw = geminiClient.generateText(prompt);
        return transactionJsonParser.parse(raw);
    }

    @Override
    public InsightsResponse generateInsights(String userId, InsightsRequest request) {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        String prompt = promptBuilder.buildInsightsPrompt(
                request.getTotalIncome(),
                request.getTotalExpense(),
                request.getTotalDebt(),
                request.getRecentSpendingPattern(),
                request.getTimeRange(),
                todayUtc
        );
        try {
            String raw = geminiClient.generateText(prompt);
            InsightsResponse parsed = insightsJsonParser.parse(raw);
            if ((parsed.getAlerts() == null || parsed.getAlerts().isEmpty()) &&
                    (parsed.getTips() == null || parsed.getTips().isEmpty())) {
                return fallbackInsights(request);
            }
            return parsed;
        } catch (Exception e) {
            return fallbackInsights(request);
        }
    }

    private InsightsResponse fallbackInsights(InsightsRequest request) {
        String alert = request.getTotalExpense() > request.getTotalIncome()
                ? "Chi tiêu của bạn đang lớn hơn thu nhập trong giai đoạn này."
                : "Chi tiêu của bạn đang trong ngưỡng an toàn so với thu nhập.";

        String debtAlert = request.getTotalDebt() > 0
                ? "Bạn đang có các khoản nợ cần theo dõi cẩn thận."
                : "Hiện tại bạn không có khoản nợ nào được báo cáo.";

        return InsightsResponse.builder()
                .alerts(java.util.List.of(alert, debtAlert))
                .tips(Collections.singletonList(
                        "Hãy thiết lập ngân sách cho các nhóm chi tiêu lớn như ăn uống, giải trí để kiểm soát tốt hơn."))
                .build();
    }

    @Override
    public AiPlanResponse plan(String userId, AiPlanRequest request) {
        String prompt = promptBuilder.buildPlanPrompt(request.getPrompt(), request.getLocale(), request.getTimezone());
        try {
            String raw = geminiClient.generateText(prompt);
            DataRequest dataRequest = planJsonParser.parse(raw);
            return AiPlanResponse.builder().dataRequest(dataRequest).build();
        } catch (Exception e) {
            String p = request.getPrompt() == null ? "" : request.getPrompt().toLowerCase();
            DataRequest fallback = DataRequest.builder()
                    .analysisType(p.contains("so sánh") ? "COMPARE_MONTHS" : (p.contains("dự đoán") ? "FORECAST_MONTH" : "GENERIC"))
                    .timeRange(p.contains("3 tháng") ? "LAST_3_MONTHS" : "THIS_MONTH")
                    .requiredDatasets(java.util.List.of(DatasetType.MONTHLY_TOTALS, DatasetType.TOP_CATEGORIES))
                    .topK(5)
                    .build();
            return AiPlanResponse.builder().dataRequest(fallback).build();
        }
    }

    @Override
    public AiAnswerResponse answer(String userId, AiAnswerRequest request) {
        String contextHeader = memoryService.buildContextHeader(userId);
        String recent = memoryService.buildRecentVisibleTranscript(userId);
        String prompt = promptBuilder.buildAnswerPrompt(
                (contextHeader.isBlank() ? "" : contextHeader + "\n\n")
                        + (recent.isBlank() ? "" : ("Recent messages:\n" + recent + "\n\n"))
                        + request.getPrompt(),
                request.getLocale(),
                request.getTimezone(),
                request.getCurrencyCode(),
                request.getDataRequest(),
                request.getDataResponse()
        );
        try {
            // Persist analysis question into the same chat history stream (exclude parse-transaction by design).
            AiChatMessage userMessage = AiChatMessage.builder()
                    .userId(userId)
                    .role(MessageRole.USER)
                    .content(request.getPrompt())
                    .createdAt(OffsetDateTime.now())
                    .build();
            chatMessageRepository.save(userMessage);

            String raw = geminiClient.generateText(prompt);
            AiAnswerResponse parsed = answerJsonParser.parse(raw);

            AiChatMessage assistantMessage = AiChatMessage.builder()
                    .userId(userId)
                    .role(MessageRole.ASSISTANT)
                    .content(parsed.getAnswerMarkdown())
                    .createdAt(OffsetDateTime.now())
                    .build();
            chatMessageRepository.save(assistantMessage);
            memoryService.updateMemoryIfNeeded(userId);

            return parsed;
        } catch (Exception e) {
            // Fallback to plain text (still returned as answerMarkdown)
            String fallback = "Mình chưa phân tích được tự động lúc này. Bạn thử lại sau hoặc cung cấp thêm dữ liệu.";

            AiChatMessage assistantMessage = AiChatMessage.builder()
                    .userId(userId)
                    .role(MessageRole.ASSISTANT)
                    .content(fallback)
                    .createdAt(OffsetDateTime.now())
                    .build();
            chatMessageRepository.save(assistantMessage);
            memoryService.updateMemoryIfNeeded(userId);

            return AiAnswerResponse.builder()
                    .answerMarkdown(fallback)
                    .actions(Collections.emptyList())
                    .build();
        }
    }

    @Override
    public List<ChatHistoryDto> getHistory(String userId) {
        // Return chronological order for UI rendering.
        return chatMessageRepository.findTop50ByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .sorted(Comparator.comparing(AiChatMessage::getCreatedAt))
                .map(m -> ChatHistoryDto.builder()
                        .role(m.getRole().name())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public void clearHistory(String userId) {
        chatMessageRepository.softDeleteAll(userId, OffsetDateTime.now());
    }
}



