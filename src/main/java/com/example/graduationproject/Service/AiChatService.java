package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Request.AiChatRequest;
import com.example.graduationproject.Dto.Request.CreateTransactionRequest;
import com.example.graduationproject.Dto.Response.*;
import com.example.graduationproject.Entity.*;
import com.example.graduationproject.Entity.Enum.AccountTier;
import com.example.graduationproject.Entity.Enum.TransactionType;
import com.example.graduationproject.Repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final GeminiClient geminiClient;
    private final ChatMessageRepository chatMessageRepository;
    private final AiLogRepository aiLogRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final BudgetRepository budgetRepository;
    private final LoanRepository loanRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    @Value("${gemini.max-messages-per-day:50}")
    private int maxMessagesPerDay;

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. Chat — endpoint chính
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public AiChatResponse chat(String email, AiChatRequest request) {
        User user = getUserByEmail(email);

        // ─── Kiểm tra Premium ──────────────────────────────────────────────
        if (user.getAccountTier() != AccountTier.PREMIUM) {
            throw new RuntimeException("PREMIUM_REQUIRED");
        }

        // ─── Kiểm tra rate limit ───────────────────────────────────────────
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long usedToday = chatMessageRepository.countByUserIdAndRoleAndCreatedAtAfter(
                user.getId(), "user", startOfDay);

        if (usedToday >= maxMessagesPerDay) {
            throw new RuntimeException("Bạn đã đạt giới hạn " + maxMessagesPerDay
                    + " tin nhắn/ngày. Vui lòng thử lại vào ngày mai.");
        }

        // ─── Session management ────────────────────────────────────────────
        UUID sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID();

        // ─── Build system prompt với context tài chính ─────────────────────
        String systemPrompt = buildSystemPrompt(user);

        // ─── Lấy lịch sử chat của session ─────────────────────────────────
        List<com.example.graduationproject.Entity.ChatMessage> history =
                chatMessageRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(
                        user.getId(), sessionId);

        // Chuyển sang MessagePair cho GeminiClient
        List<GeminiClient.MessagePair> messagePairs = new ArrayList<>();
        history.forEach(m -> messagePairs.add(
                new GeminiClient.MessagePair(m.getRole(), m.getContent())));

        // Thêm tin nhắn hiện tại của user
        messagePairs.add(new GeminiClient.MessagePair("user", request.getMessage()));

        // ─── Gọi Gemini ───────────────────────────────────────────────────
        long startMs = System.currentTimeMillis();
        String requestType = detectRequestType(request.getMessage());
        GeminiClient.GeminiResult result;
        AiLog.AiLogBuilder logBuilder = AiLog.builder()
                .user(user)
                .requestType(requestType)
                .model("gemini-2.0-flash");

        try {
            result = geminiClient.generateContent(systemPrompt, messagePairs);
            logBuilder
                    .status("SUCCESS")
                    .promptTokens(result.promptTokens())
                    .responseTokens(result.responseTokens())
                    .durationMs(System.currentTimeMillis() - startMs);
        } catch (Exception e) {
            logBuilder
                    .status("ERROR")
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startMs);
            aiLogRepository.save(logBuilder.build());
            throw e;
        }

        aiLogRepository.save(logBuilder.build());

        // ─── Lưu tin nhắn vào DB ──────────────────────────────────────────
        saveMessage(user, sessionId, "user", request.getMessage());
        saveMessage(user, sessionId, "assistant", result.text());

        // ─── Parse action nếu AI trả về CREATE_TRANSACTION ────────────────
        TransactionResponse createdTx = null;
        try {
            createdTx = parseAndExecuteAction(result.text(), email);
        } catch (Exception e) {
            log.warn("Không parse được action từ AI response: {}", e.getMessage());
        }

        return AiChatResponse.builder()
                .sessionId(sessionId)
                .reply(cleanReply(result.text()))
                .createdTransaction(createdTx)
                .usedToday((int) usedToday + 1)
                .dailyLimit(maxMessagesPerDay)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. Danh sách sessions
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getSessions(String email) {
        User user = getUserByEmail(email);
        checkPremium(user);

        List<UUID> sessionIds = chatMessageRepository.findDistinctSessionIdsByUserId(user.getId());

        return sessionIds.stream().map(sid -> {
            List<com.example.graduationproject.Entity.ChatMessage> msgs =
                    chatMessageRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(user.getId(), sid);

            String firstMsg = msgs.stream()
                    .filter(m -> "user".equals(m.getRole()))
                    .findFirst()
                    .map(com.example.graduationproject.Entity.ChatMessage::getContent)
                    .orElse("(Phiên chat)");

            // Truncate preview
            if (firstMsg.length() > 80) firstMsg = firstMsg.substring(0, 77) + "...";

            return ChatSessionResponse.builder()
                    .sessionId(sid)
                    .firstMessage(firstMsg)
                    .startedAt(msgs.isEmpty() ? null : msgs.get(0).getCreatedAt())
                    .messageCount(msgs.size())
                    .build();
        }).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. Lịch sử chat của 1 session
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getSessionHistory(String email, UUID sessionId) {
        User user = getUserByEmail(email);
        checkPremium(user);

        return chatMessageRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(
                        user.getId(), sessionId)
                .stream()
                .map(m -> ChatMessageDto.builder()
                        .id(m.getId())
                        .role(m.getRole())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. Xóa session
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public void deleteSession(String email, UUID sessionId) {
        User user = getUserByEmail(email);
        checkPremium(user);
        chatMessageRepository.deleteByUserIdAndSessionId(user.getId(), sessionId);
        log.info("User {} đã xóa session {}", email, sessionId);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Private: Build System Prompt
    // ═══════════════════════════════════════════════════════════════════════════

    private String buildSystemPrompt(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Bạn là FinBot — trợ lý tài chính AI thông minh, thân thiện.
                Hãy trả lời bằng tiếng Việt, ngắn gọn, có số liệu cụ thể khi có thể.
                
                """);

        // Ví tiền
        List<Wallet> wallets = walletRepository.findByUserId(user.getId())
                .stream().filter(w -> !Boolean.TRUE.equals(w.getIsDeleted())).toList();
        sb.append("=== VÍ TIỀN ===\n");
        BigDecimal totalBalance = BigDecimal.ZERO;
        for (Wallet w : wallets) {
            sb.append(String.format("- %s (%s): %s VND [id: %s]\n",
                    w.getName(), w.getWalletType(), w.getBalance().toPlainString(), w.getId()));
            totalBalance = totalBalance.add(w.getBalance());
        }
        sb.append("→ Tổng tài sản: ").append(totalBalance.toPlainString()).append(" VND\n\n");

        // Budget tháng hiện tại
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year);
        if (!budgets.isEmpty()) {
            sb.append("=== NGÂN SÁCH THÁNG ").append(month).append("/").append(year).append(" ===\n");
            for (Budget b : budgets) {
                BigDecimal spent = budgetRepository.calculateSpent(
                        user.getId(), b.getCategory().getId(), month, year);
                if (spent == null) spent = BigDecimal.ZERO;
                sb.append(String.format("- %s: hạn mức %s VND, đã chi %s VND [categoryId: %s]\n",
                        b.getCategory().getName(), b.getLimit().toPlainString(),
                        spent.toPlainString(), b.getCategory().getId()));
            }
            sb.append("\n");
        }

        // Loan (ACTIVE & OVERDUE)
        var loans = loanRepository.findByUserIdAndIsDeletedFalse(user.getId())
                .stream()
                .filter(l -> l.getStatus().name().equals("ACTIVE") || l.getStatus().name().equals("OVERDUE"))
                .toList();
        if (!loans.isEmpty()) {
            sb.append("=== KHOẢN VAY ===\n");
            for (var loan : loans) {
                sb.append(String.format("- %s: %s VND [%s] — %s\n",
                        loan.getCounterpart(), loan.getPrincipalAmount().toPlainString(),
                        loan.getLoanType(), loan.getStatus()));
            }
            sb.append("\n");
        }

        // Mục tiêu tài chính
        var goals = financialGoalRepository.findByUserIdAndIsDeletedFalse(user.getId())
                .stream()
                .filter(g -> g.getStatus().name().equals("IN_PROGRESS"))
                .toList();
        if (!goals.isEmpty()) {
            sb.append("=== MỤC TIÊU TÀI CHÍNH ===\n");
            for (var goal : goals) {
                sb.append(String.format("- %s: mục tiêu %s VND, đã tích lũy %s VND\n",
                        goal.getName(), goal.getTargetAmount().toPlainString(),
                        goal.getCurrentAmount().toPlainString()));
            }
            sb.append("\n");
        }

        sb.append("""
                === HƯỚNG DẪN TẠO GIAO DỊCH ===
                Khi user muốn ghi giao dịch (ví dụ: "Ăn trưa 50k", "Nhận lương 15 triệu", "Mua sách 200k"):
                1. Hỏi lại nếu thiếu thông tin cần thiết (ví, danh mục).
                2. Nếu đủ thông tin, trả lời tự nhiên VÀ kèm JSON trong block sau:
                ```transaction
                {"action":"CREATE_TRANSACTION","walletId":"<id>","categoryId":"<id hoặc null>","amount":<số>,"transactionType":"EXPENSE hoặc INCOME","note":"<ghi chú>"}
                ```
                Chỉ tạo 1 giao dịch mỗi lần. Không tự bịa walletId hay categoryId — dùng đúng id ở trên.
                """);

        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Private: Parse và thực thi CREATE_TRANSACTION action
    // ═══════════════════════════════════════════════════════════════════════════

    private TransactionResponse parseAndExecuteAction(String reply, String email) throws Exception {
        if (!reply.contains("```transaction")) return null;

        int start = reply.indexOf("```transaction") + "```transaction".length();
        int end = reply.indexOf("```", start);
        if (end <= start) return null;

        String json = reply.substring(start, end).trim();
        JsonNode node = objectMapper.readTree(json);

        if (!"CREATE_TRANSACTION".equals(node.path("action").asText())) return null;

        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setWalletId(UUID.fromString(node.path("walletId").asText()));
        req.setAmount(new BigDecimal(node.path("amount").asText()));
        req.setTransactionType(TransactionType.valueOf(node.path("transactionType").asText()));
        req.setNote(node.path("note").asText(""));
        req.setTransactionDate(LocalDateTime.now());

        String catId = node.path("categoryId").asText("null");
        if (!"null".equals(catId) && !catId.isBlank()) {
            req.setCategoryId(UUID.fromString(catId));
        }

        log.info("AI tạo giao dịch cho user {}: {} {} VND",
                email, req.getTransactionType(), req.getAmount());

        return transactionService.createTransaction(req);
    }

    // ─── Xóa block ```transaction ... ``` khỏi reply hiển thị cho user ──────
    private String cleanReply(String reply) {
        if (reply == null) return "";
        return reply.replaceAll("```transaction[\\s\\S]*?```", "").trim();
    }

    // ─── Phát hiện loại request ─────────────────────────────────────────────
    private String detectRequestType(String message) {
        String lower = message.toLowerCase();
        if (lower.matches(".*\\b(k|nghìn|triệu|đồng|chi|mua|ăn|uống|lương|thu nhập)\\b.*")) {
            return "QUICK_INPUT";
        }
        if (lower.contains("phân tích") || lower.contains("xu hướng") || lower.contains("tư vấn")) {
            return "ANALYSIS";
        }
        return "CHAT";
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void saveMessage(User user, UUID sessionId, String role, String content) {
        com.example.graduationproject.Entity.ChatMessage msg =
                com.example.graduationproject.Entity.ChatMessage.builder()
                        .user(user)
                        .sessionId(sessionId)
                        .role(role)
                        .content(content)
                        .build();
        chatMessageRepository.save(msg);
    }

    private void checkPremium(User user) {
        if (user.getAccountTier() != AccountTier.PREMIUM) {
            throw new RuntimeException("PREMIUM_REQUIRED");
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
