package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Response.*;
import com.example.graduationproject.Entity.Enum.AccountTier;
import com.example.graduationproject.Entity.Enum.PremiumOrderStatus;
import com.example.graduationproject.Entity.PremiumOrder;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Repository.BudgetRepository;
import com.example.graduationproject.Repository.FinancialGoalRepository;
import com.example.graduationproject.Repository.LoanRepository;
import com.example.graduationproject.Repository.PremiumOrderRepository;
import com.example.graduationproject.Repository.TransactionRepository;
import com.example.graduationproject.Repository.UserRepository;
import com.example.graduationproject.Repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PremiumOrderRepository premiumOrderRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final LoanRepository loanRepository;
    private final BudgetRepository budgetRepository;

    // ===================================================
    // 1. Quản lý người dùng
    // ===================================================

    /**
     * Danh sách users có phân trang và tìm kiếm theo email.
     *
     * @param emailQuery chuỗi tìm kiếm (có thể rỗng → lấy tất cả)
     * @param page       số trang (0-indexed)
     * @param size       số phần tử mỗi trang
     * @return Page<AdminUserResponse>
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(String emailQuery, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users;

        if (emailQuery != null && !emailQuery.isBlank()) {
            users = userRepository.findByEmailContainingIgnoreCase(emailQuery.trim(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.map(this::toUserResponse);
    }

    /**
     * Chi tiết một user theo ID.
     */
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(UUID userId) {
        User user = findUser(userId);
        return toUserDetailResponse(user);
    }

    /**
     * Ban / Unban tài khoản người dùng.
     *
     * @param userId ID của user cần thay đổi
     * @param ban    true = ban, false = unban
     */
    @Transactional
    public AdminUserResponse setBanStatus(UUID userId, boolean ban) {
        User user = findUser(userId);

        if (user.getRole().name().equals("ADMIN")) {
            throw new IllegalArgumentException("Không thể ban tài khoản Admin.");
        }

        user.setBanned(ban);
        userRepository.save(user);

        log.info("Admin {} tài khoản: {}", ban ? "BAN" : "UNBAN", user.getEmail());
        return toUserResponse(user);
    }

    // ===================================================
    // 2. Thống kê hệ thống (Dashboard)
    // ===================================================

    /**
     * Trả về các số liệu tổng quan cho admin dashboard.
     * Bao gồm: tổng user, phân loại tier, doanh thu, user mới 7 ngày gần nhất.
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse getSystemStats() {
        long totalUsers = userRepository.count();
        long totalBasic = userRepository.countByAccountTier(AccountTier.BASIC);
        long totalPremium = userRepository.countByAccountTier(AccountTier.PREMIUM);
        long totalBanned = userRepository.countBannedUsers();

        long totalOrders = premiumOrderRepository.count();
        long completedOrders = premiumOrderRepository.countByStatus(PremiumOrderStatus.COMPLETED);
        Long totalRevenue = premiumOrderRepository.sumCompletedRevenue();

        // User mới 7 ngày gần nhất
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from7 = now.minusDays(6).toLocalDate().atStartOfDay();
        LocalDateTime from30 = now.minusDays(29).toLocalDate().atStartOfDay();

        List<AdminStatsResponse.DailyNewUserStat> newUsersLast7Days = buildDailyUserStats(from7, now);
        List<AdminStatsResponse.DailyNewUserStat> newUsersLast30Days = buildDailyUserStats(from30, now);

        // Feature usage
        AdminStatsResponse.FeatureUsageStat featureUsage = AdminStatsResponse.FeatureUsageStat.builder()
                .totalTransactions(transactionRepository.count())
                .totalWallets(walletRepository.count())
                .totalBudgets(budgetRepository.count())
                .totalLoans(loanRepository.count())
                .totalGoals(financialGoalRepository.count())
                .build();

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalBasicUsers(totalBasic)
                .totalPremiumUsers(totalPremium)
                .totalBannedUsers(totalBanned)
                .totalPremiumOrders(totalOrders)
                .totalCompletedOrders(completedOrders)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0L)
                .newUsersLast7Days(newUsersLast7Days)
                .newUsersLast30Days(newUsersLast30Days)
                .featureUsage(featureUsage)
                .build();
    }

    // ===================================================
    // 3. Quản lý đơn hàng Premium
    // ===================================================

    /**
     * Danh sách tất cả đơn Premium, có phân trang và lọc theo status.
     *
     * @param status lọc theo trạng thái (null = lấy tất cả)
     * @param page   số trang (0-indexed)
     * @param size   số phần tử mỗi trang
     */
    @Transactional(readOnly = true)
    public Page<AdminPremiumOrderResponse> getPremiumOrders(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PremiumOrder> orders;

        if (status != null && !status.isBlank()) {
            try {
                PremiumOrderStatus orderStatus = PremiumOrderStatus.valueOf(status.toUpperCase());
                orders = premiumOrderRepository.findByStatus(orderStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ: " + status +
                        ". Các giá trị hợp lệ: PENDING, COMPLETED, FAILED, EXPIRED");
            }
        } else {
            orders = premiumOrderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return orders.map(this::toAdminPremiumOrderResponse);
    }

    /**
     * Thống kê doanh thu Premium theo năm.
     *
     * @param year năm cần thống kê (ví dụ: 2026)
     */
    @Transactional(readOnly = true)
    public AdminRevenueResponse getRevenue(int year) {
        Long totalRevenue = premiumOrderRepository.sumCompletedRevenue();
        List<Object[]> rawMonthly = premiumOrderRepository.revenueByMonth(year);

        // Tính doanh thu tháng hiện tại và năm hiện tại
        LocalDate now = LocalDate.now();
        long monthlyRevenue = 0L;
        long yearlyRevenue = 0L;

        List<AdminRevenueResponse.MonthlyRevenueStat> breakdown = new ArrayList<>();
        for (Object[] row : rawMonthly) {
            int y = ((Number) row[0]).intValue();
            int m = ((Number) row[1]).intValue();
            long rev = ((Number) row[2]).longValue();
            long cnt = ((Number) row[3]).longValue();

            breakdown.add(AdminRevenueResponse.MonthlyRevenueStat.builder()
                    .year(y).month(m).revenue(rev).orderCount(cnt)
                    .build());

            yearlyRevenue += rev;
            if (y == now.getYear() && m == now.getMonthValue()) {
                monthlyRevenue = rev;
            }
        }

        return AdminRevenueResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : 0L)
                .monthlyRevenue(monthlyRevenue)
                .yearlyRevenue(yearlyRevenue)
                .monthlyBreakdown(breakdown)
                .build();
    }

    // ===================================================
    // 3. Thống kê xu hướng nâng cao (Trends)
    // ===================================================

    /**
     * Trả về dashboard trends cho admin:
     * - Tần suất dùng tính năng (số lượng entity toàn hệ thống)
     * - Tăng trưởng user theo từng tháng trong năm chỉ định
     * - Xu hướng tài chính ẩn danh (thu/chi trung bình, top danh mục)
     * - Tổng tiền trong hệ thống
     *
     * @param year Năm cần xem thống kê tăng trưởng user
     */
    @Transactional(readOnly = true)
    public AdminTrendsResponse getTrends(int year) {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();

        // 1. Feature usage
        AdminTrendsResponse.FeatureUsageStat featureUsage = AdminTrendsResponse.FeatureUsageStat.builder()
                .totalTransactions(transactionRepository.count())
                .totalWallets(walletRepository.count())
                .totalBudgets(budgetRepository.count())
                .totalLoans(loanRepository.count())
                .totalGoals(financialGoalRepository.count())
                .build();

        // 2. User mới theo từng tháng trong năm
        List<Object[]> rawMonthly = userRepository.countNewUsersByMonth(year);
        long[] monthlyCount = new long[13]; // index 1-12
        for (Object[] row : rawMonthly) {
            int m = ((Number) row[0]).intValue();
            long cnt = ((Number) row[1]).longValue();
            monthlyCount[m] = cnt;
        }
        List<AdminTrendsResponse.MonthlyNewUserStat> newUsersByMonth = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            newUsersByMonth.add(AdminTrendsResponse.MonthlyNewUserStat.builder()
                    .month(m).year(year).count(monthlyCount[m])
                    .build());
        }

        // 3. Tài chính tính toàn platform tháng hiện tại
        Object rawResult = transactionRepository.platformIncomeExpenseByMonth(currentMonth, currentYear);
        java.math.BigDecimal platformIncome = java.math.BigDecimal.ZERO;
        java.math.BigDecimal platformExpense = java.math.BigDecimal.ZERO;

        if (rawResult != null) {
            Object[] ieRow;
            if (rawResult instanceof Object[] arr && arr.length > 0 && arr[0] instanceof Object[]) {
                // Nested: [[income, expense]]
                ieRow = (Object[]) arr[0];
            } else {
                // Flat: [income, expense]
                ieRow = (Object[]) rawResult;
            }
            if (ieRow.length >= 2) {
                platformIncome = ieRow[0] != null ? (java.math.BigDecimal) ieRow[0] : java.math.BigDecimal.ZERO;
                platformExpense = ieRow[1] != null ? (java.math.BigDecimal) ieRow[1] : java.math.BigDecimal.ZERO;
            }
        }

        long activeUsers = userRepository.countByAccountTier(AccountTier.BASIC)
                + userRepository.countByAccountTier(AccountTier.PREMIUM);

        java.math.BigDecimal avgIncome = java.math.BigDecimal.ZERO;
        java.math.BigDecimal avgExpense = java.math.BigDecimal.ZERO;
        if (activeUsers > 0) {
            avgIncome = platformIncome.divide(java.math.BigDecimal.valueOf(activeUsers), 0,
                    java.math.RoundingMode.HALF_UP);
            avgExpense = platformExpense.divide(java.math.BigDecimal.valueOf(activeUsers), 0,
                    java.math.RoundingMode.HALF_UP);
        }

        AdminTrendsResponse.PlatformFinancialStat currentMonthFinancial = AdminTrendsResponse.PlatformFinancialStat
                .builder()
                .month(currentMonth)
                .year(currentYear)
                .totalIncome(platformIncome)
                .totalExpense(platformExpense)
                .netCashFlow(platformIncome.subtract(platformExpense))
                .avgIncomePerUser(avgIncome)
                .avgExpensePerUser(avgExpense)
                .build();

        // 4. Top 5 danh mục chi tiêu toàn platform
        List<Object[]> rawTopCats = transactionRepository.topExpenseCategoriesAllUsers();
        List<AdminTrendsResponse.TopCategoryStat> topExpenseCategories = rawTopCats.stream()
                .limit(5)
                .map(row -> AdminTrendsResponse.TopCategoryStat.builder()
                        .categoryName((String) row[0])
                        .totalAmount((java.math.BigDecimal) row[1])
                        .transactionCount(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        // 5. Tổng tiền trong hệ thống
        java.math.BigDecimal totalMoney = walletRepository.sumBalanceAllWallets();

        return AdminTrendsResponse.builder()
                .year(year)
                .featureUsage(featureUsage)
                .newUsersByMonth(newUsersByMonth)
                .currentMonthFinancial(currentMonthFinancial)
                .topExpenseCategories(topExpenseCategories)
                .totalMoneyInSystem(totalMoney)
                .build();
    }

    // ===================================================
    // Private helpers
    // ===================================================

    /**
     * Build danh sách thống kê user mới theo ngày, điền 0 vào những ngày không có.
     */
    private List<AdminStatsResponse.DailyNewUserStat> buildDailyUserStats(
            LocalDateTime from, LocalDateTime to) {

        List<Object[]> rawData = userRepository.countNewUsersByDay(from, to);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Khởi tạo map với tất cả ngày = 0
        Map<String, Long> countByDate = new LinkedHashMap<>();
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()) + 1;
        for (long i = totalDays - 1; i >= 0; i--) {
            String dateStr = to.minusDays(i).toLocalDate().format(fmt);
            countByDate.put(dateStr, 0L);
        }
        for (Object[] row : rawData) {
            countByDate.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return countByDate.entrySet().stream()
                .map(e -> AdminStatsResponse.DailyNewUserStat.builder()
                        .date(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy user với ID: " + userId));
    }

    private AdminUserResponse toUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .accountTier(user.getAccountTier().name())
                .isBanned(user.isBanned())
                .premiumExpiredAt(user.getPremiumExpiredAt())
                .walletCount(user.getWallets().size())
                .transactionCount(user.getWallets().stream()
                        .mapToInt(w -> w.getTransactions().size())
                        .sum())
                .build();
    }

    private AdminUserDetailResponse toUserDetailResponse(User user) {
        int transactionCount = user.getWallets().stream()
                .mapToInt(w -> w.getTransactions().size())
                .sum();

        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .googleId(user.getGoogleId())
                .role(user.getRole().name())
                .accountTier(user.getAccountTier().name())
                .isBanned(user.isBanned())
                .premiumExpiredAt(user.getPremiumExpiredAt())
                .walletCount(user.getWallets().size())
                .transactionCount(transactionCount)
                .loanCount(user.getLoans().size())
                .goalCount((int) financialGoalRepository.countByUserId(user.getId()))
                .budgetCount(user.getBudgets().size())
                .premiumOrderCount(user.getPremiumOrders().size())
                .build();
    }

    private AdminPremiumOrderResponse toAdminPremiumOrderResponse(PremiumOrder order) {
        return AdminPremiumOrderResponse.builder()
                .id(order.getId())
                .userEmail(order.getUser().getEmail())
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
