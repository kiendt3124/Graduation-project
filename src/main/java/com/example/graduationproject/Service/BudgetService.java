package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Request.BudgetRequest;
import com.example.graduationproject.Dto.Response.BudgetResponse;
import com.example.graduationproject.Entity.Budget;
import com.example.graduationproject.Entity.Category;
import com.example.graduationproject.Entity.Enum.TransactionType;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Repository.BudgetRepository;
import com.example.graduationproject.Repository.CategoryRepository;
import com.example.graduationproject.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Transactional
    public BudgetResponse createBudget(String email, BudgetRequest.Create req) {
        User user = getUserByEmail(email);

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Chỉ cho phép tạo budget cho category EXPENSE
        if (!TransactionType.EXPENSE.equals(category.getTransactionType())) {
            throw new RuntimeException("Budget can only be created for EXPENSE categories");
        }

        // Kiểm tra không tạo trùng (cùng user + category + tháng + năm)
        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(
                user.getId(), category.getId(), req.getMonth(), req.getYear())) {
            throw new RuntimeException(
                    "Budget already exists for category '" + category.getName()
                    + "' in " + req.getMonth() + "/" + req.getYear());
        }

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .limit(req.getAmount())
                .month(req.getMonth())
                .year(req.getYear())
                .build();

        budgetRepository.save(budget);
        return toResponse(budget, user.getId());
    }

    // ─── GET BY MONTH ─────────────────────────────────────────────────────────

    /**
     * Lấy tất cả budget của user trong 1 tháng/năm.
     * Sort: budget gần vượt mức (progressPercentage cao) lên đầu.
     */
    public List<BudgetResponse> getBudgetsByMonth(String email, int month, int year) {
        User user = getUserByEmail(email);

        return budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year)
                .stream()
                .map(b -> toResponse(b, user.getId()))
                .sorted(Comparator.comparing(BudgetResponse::getProgressPercentage).reversed())
                .collect(Collectors.toList());
    }

    // ─── GET ALL ──────────────────────────────────────────────────────────────

    /**
     * Lấy toàn bộ budget của user (mọi tháng, mọi năm).
     * Sort: năm giảm dần → tháng giảm dần → progressPercentage giảm dần.
     */
    public List<BudgetResponse> getAllBudgets(String email) {
        User user = getUserByEmail(email);

        return budgetRepository.findByUserId(user.getId())
                .stream()
                .map(b -> toResponse(b, user.getId()))
                .sorted(Comparator
                        .comparingInt(BudgetResponse::getYear).reversed()
                        .thenComparingInt(BudgetResponse::getMonth).reversed()
                        .thenComparing(Comparator.comparing(BudgetResponse::getProgressPercentage).reversed()))
                .collect(Collectors.toList());
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @Transactional
    public BudgetResponse updateBudget(String email, BudgetRequest.Update req) {
        User user = getUserByEmail(email);

        Budget budget = budgetRepository.findById(req.getId())
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        // Kiểm tra budget có thuộc về user đang đăng nhập không
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        budget.setLimit(req.getAmount());
        budgetRepository.save(budget);
        return toResponse(budget, user.getId());
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Transactional
    public void deleteBudget(String email, UUID id) {
        User user = getUserByEmail(email);

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        // Kiểm tra budget có thuộc về user đang đăng nhập không
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        budgetRepository.delete(budget);
    }

    // ─── HELPER: Entity → Response ────────────────────────────────────────────

    /**
     * Map Budget entity sang BudgetResponse.
     * Tính spent real-time từ bảng transactions (Option A).
     */
    private BudgetResponse toResponse(Budget budget, UUID userId) {
        BigDecimal spentAmount = budgetRepository.calculateSpent(
                userId,
                budget.getCategory().getId(),
                budget.getMonth(),
                budget.getYear());

        BigDecimal limitAmount = budget.getLimit();
        BigDecimal remainingAmount = limitAmount.subtract(spentAmount);

        // Tính % sử dụng, làm tròn 2 chữ số thập phân
        BigDecimal progressPercentage;
        if (limitAmount.compareTo(BigDecimal.ZERO) == 0) {
            progressPercentage = BigDecimal.ZERO;
        } else {
            progressPercentage = spentAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(limitAmount, 2, RoundingMode.HALF_UP);
        }

        // Tính số tiền an toàn có thể tiêu mỗi ngày còn lại trong tháng
        BigDecimal dailySafeToSpend = calculateDailySafeToSpend(
                remainingAmount, budget.getMonth(), budget.getYear());

        boolean isOverBudget = spentAmount.compareTo(limitAmount) >= 0;

        return BudgetResponse.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .categoryIcon(budget.getCategory().getIcon())
                .month(budget.getMonth())
                .year(budget.getYear())
                .limitAmount(limitAmount)
                .spentAmount(spentAmount)
                .remainingAmount(remainingAmount)
                .progressPercentage(progressPercentage)
                .dailySafeToSpend(dailySafeToSpend)
                .isOverBudget(isOverBudget)
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }

    /**
     * Tính trung bình số tiền có thể tiêu mỗi ngày cho đến hết tháng.
     * = remainingAmount / số ngày còn lại của tháng (tính từ hôm nay).
     *
     * Trả về 0 nếu:
     * - Tháng đã qua
     * - Ngân sách đã bị vượt (remaining <= 0)
     */
    private BigDecimal calculateDailySafeToSpend(BigDecimal remainingAmount, int month, int year) {
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        LocalDate today = LocalDate.now();
        LocalDate budgetMonth = LocalDate.of(year, month, 1);

        // Nếu tháng ngân sách đã qua → trả về 0
        if (budgetMonth.getYear() < today.getYear()
                || (budgetMonth.getYear() == today.getYear() && budgetMonth.getMonthValue() < today.getMonthValue())) {
            return BigDecimal.ZERO;
        }

        // Xác định ngày bắt đầu tính (hôm nay hoặc ngày 1 nếu tháng tương lai)
        LocalDate startDate = budgetMonth.isAfter(today) ? budgetMonth : today;

        // Ngày cuối tháng
        LocalDate lastDayOfMonth = budgetMonth.withDayOfMonth(budgetMonth.lengthOfMonth());

        long daysRemaining = lastDayOfMonth.toEpochDay() - startDate.toEpochDay() + 1;

        if (daysRemaining <= 0) {
            return BigDecimal.ZERO;
        }

        return remainingAmount.divide(BigDecimal.valueOf(daysRemaining), 2, RoundingMode.HALF_UP);
    }

    // ─── HELPER: lấy User từ email ─────────────────────────────────────────────

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
