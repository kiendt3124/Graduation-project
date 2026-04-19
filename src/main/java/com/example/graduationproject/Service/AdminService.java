package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Response.*;
import com.example.graduationproject.Entity.Enum.AccountTier;
import com.example.graduationproject.Entity.Enum.PremiumOrderStatus;
import com.example.graduationproject.Entity.PremiumOrder;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Repository.PremiumOrderRepository;
import com.example.graduationproject.Repository.UserRepository;
import com.example.graduationproject.Repository.FinancialGoalRepository;
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

    // ===================================================
    // 1. Quản lý người dùng
    // ===================================================

    /**
     * Danh sách users có phân trang và tìm kiếm theo email.
     *
     * @param emailQuery  chuỗi tìm kiếm (có thể rỗng → lấy tất cả)
     * @param page        số trang (0-indexed)
     * @param size        số phần tử mỗi trang
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
        LocalDateTime from = now.minusDays(6).toLocalDate().atStartOfDay();
        List<Object[]> rawData = userRepository.countNewUsersByDay(from, now);

        // Build map ngày → count để điền 0 vào các ngày không có user mới
        Map<String, Long> countByDate = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            String dateStr = now.minusDays(i).toLocalDate().format(fmt);
            countByDate.put(dateStr, 0L);
        }
        for (Object[] row : rawData) {
            String dateStr = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            countByDate.put(dateStr, count);
        }

        List<AdminStatsResponse.DailyNewUserStat> newUsersLast7Days = countByDate.entrySet().stream()
                .map(e -> AdminStatsResponse.DailyNewUserStat.builder()
                        .date(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalBasicUsers(totalBasic)
                .totalPremiumUsers(totalPremium)
                .totalBannedUsers(totalBanned)
                .totalPremiumOrders(totalOrders)
                .totalCompletedOrders(completedOrders)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0L)
                .newUsersLast7Days(newUsersLast7Days)
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
    // Private helpers
    // ===================================================

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
