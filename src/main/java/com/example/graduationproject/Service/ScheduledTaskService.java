package com.example.graduationproject.Service;

import com.example.graduationproject.Entity.*;
import com.example.graduationproject.Entity.Enum.*;
import com.example.graduationproject.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chứa toàn bộ tác vụ nền chạy tự động theo lịch.
 *
 * Task 1: Expire đơn Premium PENDING quá hạn         — mỗi 5 phút
 * Task 2: Hạ tier Premium hết hạn về BASIC            — 0:00 mỗi ngày
 * Task 3: Cập nhật Loan quá hạn thành OVERDUE         — 1:00 mỗi ngày
 * Task 4: Clone budget tháng trước → tháng mới        — 8:00 ngày 1 mỗi tháng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final PremiumOrderRepository premiumOrderRepository;
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final BudgetRepository budgetRepository;
    private final NotificationService notificationService;

    // ═══════════════════════════════════════════════════════════════════════════
    // Task 1: Expire đơn Premium PENDING đã quá hạn (mỗi 5 phút)
    // ═══════════════════════════════════════════════════════════════════════════

    @Scheduled(fixedRate = 5 * 60 * 1000) // 5 phút
    @Transactional
    public void expirePendingPremiumOrders() {
        LocalDateTime now = LocalDateTime.now();

        List<PremiumOrder> expiredOrders = premiumOrderRepository
                .findByStatusAndExpiredAtBefore(PremiumOrderStatus.PENDING, now);

        if (expiredOrders.isEmpty()) return;

        expiredOrders.forEach(order -> {
            order.setStatus(PremiumOrderStatus.EXPIRED);
            premiumOrderRepository.save(order);

            // Thông báo cho user
            notificationService.createNotification(
                    order.getUser().getId(),
                    NotificationType.ORDER_EXPIRED,
                    "Đơn Premium đã hết hạn",
                    "Đơn mua gói " + order.getPlan().name() + " (mã: " + order.getTxnRef()
                            + ") đã hết thời hạn thanh toán. Vui lòng tạo đơn mới nếu muốn nâng cấp.",
                    order.getId()
            );
        });

        log.info("[Scheduler] Đã expire {} đơn Premium PENDING quá hạn", expiredOrders.size());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Task 2: Hạ tier Premium hết hạn (0:00 mỗi ngày)
    // ═══════════════════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 0 * * *") // 0:00 mỗi ngày
    @Transactional
    public void downgradedExpiredPremiumUsers() {
        LocalDateTime now = LocalDateTime.now();

        // ─── 2a. Hạ tier user đã hết hạn ────────────────────────────────────
        List<User> expiredUsers = userRepository
                .findByAccountTierAndPremiumExpiredAtBefore(AccountTier.PREMIUM, now);

        expiredUsers.forEach(user -> {
            user.setAccountTier(AccountTier.BASIC);
            user.setPremiumExpiredAt(null);
            userRepository.save(user);

            notificationService.createNotification(
                    user.getId(),
                    NotificationType.PREMIUM_EXPIRED,
                    "Gói Premium đã hết hạn",
                    "Tài khoản của bạn đã được chuyển về gói Basic. "
                            + "Nâng cấp lại để tiếp tục sử dụng các tính năng Premium.",
                    null
            );
        });

        if (!expiredUsers.isEmpty()) {
            log.info("[Scheduler] Đã hạ tier {} user Premium hết hạn → BASIC", expiredUsers.size());
        }

        // ─── 2b. Nhắc nhở user sắp hết hạn (trong 3 ngày tới) ──────────────
        LocalDateTime threeDaysLater = now.plusDays(3);
        List<User> expiringUsers = userRepository
                .findByAccountTierAndPremiumExpiredAtBetween(AccountTier.PREMIUM, now, threeDaysLater);

        expiringUsers.forEach(user -> {
            notificationService.createNotification(
                    user.getId(),
                    NotificationType.PREMIUM_EXPIRING_SOON,
                    "Gói Premium sắp hết hạn",
                    "Gói Premium của bạn sẽ hết hạn vào "
                            + user.getPremiumExpiredAt().toLocalDate()
                            + ". Gia hạn ngay để không bị gián đoạn dịch vụ.",
                    null
            );
        });

        if (!expiringUsers.isEmpty()) {
            log.info("[Scheduler] Đã nhắc nhở {} user Premium sắp hết hạn", expiringUsers.size());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Task 3: Cập nhật Loan quá hạn thành OVERDUE (1:00 mỗi ngày)
    // ═══════════════════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 1 * * *") // 1:00 mỗi ngày
    @Transactional
    public void updateOverdueLoans() {
        LocalDate today = LocalDate.now();

        // ─── 3a. Chuyển loan ACTIVE quá hạn → OVERDUE ───────────────────────
        List<Loan> overdueLoans = loanRepository
                .findByStatusAndIsDeletedFalseAndDueDateBefore(LoanStatus.ACTIVE, today);

        overdueLoans.forEach(loan -> {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);

            notificationService.createNotification(
                    loan.getUser().getId(),
                    NotificationType.LOAN_OVERDUE,
                    "Khoản vay đã quá hạn",
                    "Khoản vay \"" + loan.getCounterpart() + "\" ("
                            + loan.getPrincipalAmount().toPlainString()
                            + " VND) đã quá hạn thanh toán ngày " + loan.getDueDate() + ".",
                    loan.getId()
            );
        });

        if (!overdueLoans.isEmpty()) {
            log.info("[Scheduler] Đã cập nhật {} khoản vay → OVERDUE", overdueLoans.size());
        }

        // ─── 3b. Nhắc nhở loan sắp đến hạn (trong 3 ngày tới) ──────────────
        LocalDate threeDaysLater = today.plusDays(3);
        List<Loan> dueSoonLoans = loanRepository
                .findByStatusAndIsDeletedFalseAndDueDateBetween(LoanStatus.ACTIVE, today, threeDaysLater);

        dueSoonLoans.forEach(loan -> {
            notificationService.createNotification(
                    loan.getUser().getId(),
                    NotificationType.LOAN_DUE_SOON,
                    "Khoản vay sắp đến hạn",
                    "Khoản vay \"" + loan.getCounterpart() + "\" ("
                            + loan.getPrincipalAmount().toPlainString()
                            + " VND) sẽ đến hạn vào ngày " + loan.getDueDate()
                            + ". Hãy chuẩn bị thanh toán.",
                    loan.getId()
            );
        });

        if (!dueSoonLoans.isEmpty()) {
            log.info("[Scheduler] Đã nhắc nhở {} khoản vay sắp đến hạn", dueSoonLoans.size());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Task 4: Clone budget tháng trước → tháng mới (8:00 ngày 1 mỗi tháng)
    // ═══════════════════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 8 1 * *") // 8:00 ngày 1 mỗi tháng
    @Transactional
    public void rolloverBudgets() {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();

        // Tính tháng trước
        LocalDate lastMonth = today.minusMonths(1);
        int prevMonth = lastMonth.getMonthValue();
        int prevYear = lastMonth.getYear();

        // Lấy tất cả budget của tháng trước
        List<Budget> previousBudgets = budgetRepository.findByMonthAndYear(prevMonth, prevYear);

        int created = 0;
        for (Budget oldBudget : previousBudgets) {
            // Kiểm tra đã có budget cho tháng mới chưa (tránh duplicate)
            boolean exists = budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(
                    oldBudget.getUser().getId(),
                    oldBudget.getCategory().getId(),
                    currentMonth,
                    currentYear
            );

            if (!exists) {
                Budget newBudget = Budget.builder()
                        .user(oldBudget.getUser())
                        .category(oldBudget.getCategory())
                        .limit(oldBudget.getLimit())
                        .month(currentMonth)
                        .year(currentYear)
                        .build();

                budgetRepository.save(newBudget);
                created++;

                notificationService.createNotification(
                        oldBudget.getUser().getId(),
                        NotificationType.BUDGET_CREATED,
                        "Ngân sách tháng mới đã sẵn sàng",
                        "Ngân sách cho danh mục \"" + oldBudget.getCategory().getName()
                                + "\" đã được tự động tạo cho tháng " + currentMonth + "/" + currentYear
                                + " với hạn mức " + oldBudget.getLimit().toPlainString() + " VND.",
                        newBudget.getId()
                );
            }
        }

        if (created > 0) {
            log.info("[Scheduler] Đã tự động tạo {} budget cho tháng {}/{}", created, currentMonth, currentYear);
        }
    }
}
