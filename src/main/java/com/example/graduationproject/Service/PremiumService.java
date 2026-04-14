package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Response.InitiatePremiumResponse;
import com.example.graduationproject.Dto.Response.PremiumOrderResponse;
import com.example.graduationproject.Dto.Response.PremiumStatusResponse;
import com.example.graduationproject.Dto.Webhook.SePayWebhookPayload;
import com.example.graduationproject.Entity.Enum.AccountTier;
import com.example.graduationproject.Entity.Enum.PremiumOrderStatus;
import com.example.graduationproject.Entity.Enum.PremiumPlan;
import com.example.graduationproject.Entity.PremiumOrder;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Repository.PremiumOrderRepository;
import com.example.graduationproject.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumService {

    private final PremiumOrderRepository premiumOrderRepository;
    private final UserRepository userRepository;

    // ===== Cấu hình từ application.properties =====
    @Value("${bank.account-number}")
    private String bankAccountNumber;

    @Value("${bank.account-name}")
    private String bankAccountName;

    @Value("${bank.name}")
    private String bankName;

    @Value("${premium.price.monthly:49000}")
    private Long priceMonthly;

    @Value("${premium.price.yearly:399000}")
    private Long priceYearly;

    @Value("${premium.order-expiry-minutes:30}")
    private int orderExpiryMinutes;

    // ===================================================
    // 1. Tạo đơn mua Premium — trả về QR + thông tin CK
    // ===================================================
    @Transactional
    public InitiatePremiumResponse initiatePurchase(String email, PremiumPlan plan) {
        User user = getUser(email);

        long amount = (plan == PremiumPlan.MONTHLY) ? priceMonthly : priceYearly;

        // Tạo mã giao dịch duy nhất: "PREMIUM-XXXXXXXX"
        String txnRef = "PREMIUM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusMinutes(orderExpiryMinutes);

        // Lưu đơn hàng với trạng thái PENDING
        PremiumOrder order = PremiumOrder.builder()
                .user(user)
                .plan(plan)
                .status(PremiumOrderStatus.PENDING)
                .amount(amount)
                .txnRef(txnRef)
                .createdAt(now)
                .expiredAt(expiredAt)
                .build();

        premiumOrderRepository.save(order);

        // Tạo URL QR SePay — user quét bằng app ngân hàng
        // Format: https://qr.sepay.vn/img?acc={STK}&bank={BANK}&amount={amount}&des={txnRef}
        String qrUrl = String.format(
                "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s",
                bankAccountNumber, bankName, amount, txnRef
        );

        log.info("Tạo đơn Premium {} cho user {}: txnRef={}, amount={}", plan, email, txnRef, amount);

        return InitiatePremiumResponse.builder()
                .txnRef(txnRef)
                .amount(amount)
                .plan(plan.name())
                .bankName(bankName)
                .accountNumber(bankAccountNumber)
                .accountName(bankAccountName)
                .transferContent(txnRef)
                .qrUrl(qrUrl)
                .expiredAt(expiredAt)
                .build();
    }

    // ===================================================
    // 2. Xử lý webhook từ SePay (server → server)
    // ===================================================
    @Transactional
    public void handleWebhook(SePayWebhookPayload payload) {
        log.info("Nhận webhook SePay: content='{}', amount={}, type={}",
                payload.getContent(), payload.getTransferAmount(), payload.getTransferType());

        // Chỉ xử lý giao dịch tiền VÀO
        if (!"in".equalsIgnoreCase(payload.getTransferType())) {
            log.info("Bỏ qua giao dịch loại '{}' (không phải tiền vào)", payload.getTransferType());
            return;
        }

        // Tìm txnRef "PREMIUM-XXXXXXXX" trong nội dung chuyển khoản
        String content = payload.getContent();
        if (content == null || !content.contains("PREMIUM-")) {
            log.info("Nội dung CK không chứa mã PREMIUM, bỏ qua");
            return;
        }

        // Trích xuất txnRef từ nội dung (có thể có text thừa)
        String txnRef = extractTxnRef(content);
        if (txnRef == null) {
            log.warn("Không tìm được txnRef hợp lệ trong content: '{}'", content);
            return;
        }

        // Tìm đơn hàng trong DB
        PremiumOrder order = premiumOrderRepository.findByTxnRef(txnRef).orElse(null);
        if (order == null) {
            log.warn("Không tìm thấy đơn hàng với txnRef: {}", txnRef);
            return;
        }

        // Idempotent: nếu đã xử lý rồi thì bỏ qua
        if (order.getStatus() != PremiumOrderStatus.PENDING) {
            log.info("Đơn {} đã ở trạng thái {}, bỏ qua", txnRef, order.getStatus());
            return;
        }

        // Kiểm tra số tiền
        if (!order.getAmount().equals(payload.getTransferAmount())) {
            log.warn("Sai số tiền cho txnRef {}: cần {} nhưng nhận {}",
                    txnRef, order.getAmount(), payload.getTransferAmount());
            order.setStatus(PremiumOrderStatus.FAILED);
            premiumOrderRepository.save(order);
            return;
        }

        // Kích hoạt Premium
        order.setStatus(PremiumOrderStatus.COMPLETED);
        order.setPaidAt(LocalDateTime.now());
        premiumOrderRepository.save(order);

        activatePremium(order.getUser(), order.getPlan());

        log.info("Kích hoạt Premium thành công cho user {}, gói {}", order.getUser().getEmail(), order.getPlan());
    }

    // ===================================================
    // 3. Xem trạng thái Premium hiện tại
    // ===================================================
    @Transactional(readOnly = true)
    public PremiumStatusResponse getStatus(String email) {
        User user = getUser(email);
        LocalDateTime now = LocalDateTime.now();

        boolean isExpired = (user.getAccountTier() == AccountTier.PREMIUM)
                && (user.getPremiumExpiredAt() != null)
                && user.getPremiumExpiredAt().isBefore(now);

        return PremiumStatusResponse.builder()
                .tier(user.getAccountTier().name())
                .expiredAt(user.getPremiumExpiredAt())
                .expired(isExpired)
                .build();
    }

    // ===================================================
    // 4. Lịch sử đơn mua của user
    // ===================================================
    @Transactional(readOnly = true)
    public List<PremiumOrderResponse> getMyOrders(String email) {
        User user = getUser(email);
        return premiumOrderRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    // ===================================================
    // Private helpers
    // ===================================================

    /**
     * Kích hoạt hoặc gia hạn Premium cho user.
     * Nếu còn hạn → gia hạn từ ngày expiredAt hiện tại.
     * Nếu hết hạn hoặc BASIC → tính từ now.
     */
    private void activatePremium(User user, PremiumPlan plan) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base;

        boolean stillActive = (user.getAccountTier() == AccountTier.PREMIUM)
                && (user.getPremiumExpiredAt() != null)
                && user.getPremiumExpiredAt().isAfter(now);

        if (stillActive) {
            // Gia hạn tiếp từ thời điểm hết hạn hiện tại
            base = user.getPremiumExpiredAt();
        } else {
            base = now;
        }

        LocalDateTime newExpiry = (plan == PremiumPlan.MONTHLY)
                ? base.plusDays(30)
                : base.plusDays(365);

        user.setAccountTier(AccountTier.PREMIUM);
        user.setPremiumExpiredAt(newExpiry);
        userRepository.save(user);

        log.info("User {} → PREMIUM, hết hạn: {}", user.getEmail(), newExpiry);
    }

    /**
     * Trích xuất "PREMIUM-XXXXXXXX" từ nội dung chuyển khoản.
     * Ví dụ: "Chuyen tien PREMIUM-ABC12345 thanh toan" → "PREMIUM-ABC12345"
     */
    private String extractTxnRef(String content) {
        String[] parts = content.split("\\s+");
        for (String part : parts) {
            if (part.startsWith("PREMIUM-") && part.length() == 16) {
                return part;
            }
        }
        return null;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user: " + email));
    }

    private PremiumOrderResponse toOrderResponse(PremiumOrder order) {
        return PremiumOrderResponse.builder()
                .id(order.getId())
                .plan(order.getPlan().name())
                .status(order.getStatus().name())
                .amount(order.getAmount())
                .txnRef(order.getTxnRef())
                .createdAt(order.getCreatedAt())
                .expiredAt(order.getExpiredAt())
                .paidAt(order.getPaidAt())
                .build();
    }
}
