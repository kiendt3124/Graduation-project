package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Response.*;
import com.example.graduationproject.Entity.Enum.GoalStatus;
import com.example.graduationproject.Entity.Enum.LoanStatus;
import com.example.graduationproject.Entity.Enum.LoanType;
import com.example.graduationproject.Entity.Enum.TransactionType;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ReportService {

    private final UserRepository          userRepository;
    private final TransactionRepository   transactionRepository;
    private final WalletRepository        walletRepository;
    private final LoanRepository          loanRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final BudgetRepository        budgetRepository;

    // ─── 1. TỔNG QUAN TÀI CHÍNH THÁNG ────────────────────────────────────────

    /**
     * Trả về bức tranh tổng quan tài chính của user trong tháng/năm chỉ định:
     * tổng thu, tổng chi, dòng tiền ròng, tổng số dư ví, thông tin vay mượn,
     * và tiến độ mục tiêu tài chính.
     */
    public ReportSummaryResponse getSummary(String email, int month, int year) {
        UUID userId = resolveUserId(email);

        // Khoảng thời gian từ đầu đến cuối tháng
        LocalDateTime from = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime to   = from.withDayOfMonth(from.toLocalDate().lengthOfMonth())
                                 .with(LocalTime.MAX);

        BigDecimal totalIncome  = transactionRepository
                .sumByTypeAndPeriod(userId, TransactionType.INCOME, from, to);
        BigDecimal totalExpense = transactionRepository
                .sumByTypeAndPeriod(userId, TransactionType.EXPENSE, from, to);
        BigDecimal netCashFlow  = totalIncome.subtract(totalExpense);

        BigDecimal totalBalance = walletRepository.sumBalanceByUserId(userId);

        long activeLoansCount = loanRepository
                .countByUserIdAndStatusAndIsDeletedFalse(userId, LoanStatus.ACTIVE);
        BigDecimal totalDebt  = loanRepository
                .sumPrincipalByUserAndType(userId, LoanType.BORROW);
        BigDecimal totalLent  = loanRepository
                .sumPrincipalByUserAndType(userId, LoanType.LEND);

        long goalsInProgress = financialGoalRepository
                .findByUserIdAndStatusAndIsDeletedFalse(userId, GoalStatus.ACTIVE).size();
        long goalsCompleted  = financialGoalRepository
                .findByUserIdAndStatusAndIsDeletedFalse(userId, GoalStatus.COMPLETED).size();

        return ReportSummaryResponse.builder()
                .month(month)
                .year(year)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netCashFlow(netCashFlow)
                .totalBalance(totalBalance)
                .activeLoansCount(activeLoansCount)
                .totalDebt(totalDebt)
                .totalLent(totalLent)
                .goalsInProgress(goalsInProgress)
                .goalsCompleted(goalsCompleted)
                .build();
    }

    // ─── 2. CHI TIÊU THEO DANH MỤC ───────────────────────────────────────────

    /**
     * Trả về danh sách chi tiêu (EXPENSE) nhóm theo danh mục trong tháng/năm.
     * Kèm tỉ lệ % của từng danh mục so với tổng chi.
     */
    public List<CategoryExpenseResponse> getExpenseByCategory(String email, int month, int year) {
        UUID userId = resolveUserId(email);
        return buildCategoryReport(userId, TransactionType.EXPENSE, month, year);
    }

    /**
     * Trả về danh sách thu nhập (INCOME) nhóm theo danh mục trong tháng/năm.
     * Kèm tỉ lệ % của từng danh mục so với tổng thu.
     */
    public List<CategoryExpenseResponse> getIncomeByCategory(String email, int month, int year) {
        UUID userId = resolveUserId(email);
        return buildCategoryReport(userId, TransactionType.INCOME, month, year);
    }

    private List<CategoryExpenseResponse> buildCategoryReport(
            UUID userId, TransactionType type, int month, int year) {

        List<Object[]> rows = transactionRepository
                .sumGroupByCategory(userId, type, month, year);

        // Tính tổng để tính %
        BigDecimal grandTotal = rows.stream()
                .map(r -> (BigDecimal) r[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryExpenseResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            UUID       categoryId   = (UUID) row[0];
            String     categoryName = (String) row[1];
            BigDecimal total        = (BigDecimal) row[2];

            BigDecimal pct = BigDecimal.ZERO;
            if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                pct = total.multiply(BigDecimal.valueOf(100))
                           .divide(grandTotal, 2, RoundingMode.HALF_UP);
            }

            result.add(CategoryExpenseResponse.builder()
                    .categoryId(categoryId)
                    .categoryName(categoryName)
                    .totalAmount(total)
                    .percentage(pct)
                    .build());
        }
        return result;
    }

    // ─── 3. DÒNG TIỀN THEO NGÀY ──────────────────────────────────────────────

    /**
     * Trả về dòng tiền thu/chi theo từng ngày trong khoảng [from, to].
     * Những ngày không có giao dịch sẽ không xuất hiện trong kết quả
     * (mobile app tự fill 0 nếu cần).
     */
    public List<CashFlowByDayResponse> getCashFlowByDay(
            String email, LocalDate from, LocalDate to) {

        if (from == null || to == null) {
            throw new RuntimeException("from và to không được để trống");
        }
        if (from.isAfter(to)) {
            throw new RuntimeException("from phải trước hoặc bằng to");
        }

        UUID userId = resolveUserId(email);
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.atTime(LocalTime.MAX);

        List<Object[]> rows = transactionRepository.sumGroupByDay(userId, fromDt, toDt);

        List<CashFlowByDayResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate  date    = (LocalDate) row[0];
            BigDecimal income  = (BigDecimal) row[1];
            BigDecimal expense = (BigDecimal) row[2];
            BigDecimal net     = income.subtract(expense);

            result.add(CashFlowByDayResponse.builder()
                    .date(date)
                    .income(income)
                    .expense(expense)
                    .net(net)
                    .build());
        }
        return result;
    }

    // ─── 4. BÁO CÁO 12 THÁNG TRONG NĂM ─────────────────────────────────────

    /**
     * Trả về tổng thu/chi của từng tháng trong năm chỉ định.
     * Những tháng không có giao dịch sẽ trả về income=0, expense=0.
     */
    public List<MonthlyReportResponse> getMonthlyReport(String email, int year) {
        UUID userId = resolveUserId(email);
        List<Object[]> rows = transactionRepository.sumGroupByMonth(userId, year);

        // Map kết quả theo tháng
        BigDecimal[] incomeByMonth  = new BigDecimal[13];
        BigDecimal[] expenseByMonth = new BigDecimal[13];
        for (int i = 1; i <= 12; i++) {
            incomeByMonth[i]  = BigDecimal.ZERO;
            expenseByMonth[i] = BigDecimal.ZERO;
        }

        for (Object[] row : rows) {
            int        m       = ((Number) row[0]).intValue();
            BigDecimal income  = (BigDecimal) row[1];
            BigDecimal expense = (BigDecimal) row[2];
            incomeByMonth[m]  = income;
            expenseByMonth[m] = expense;
        }

        List<MonthlyReportResponse> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            BigDecimal inc = incomeByMonth[m];
            BigDecimal exp = expenseByMonth[m];
            result.add(MonthlyReportResponse.builder()
                    .month(m)
                    .year(year)
                    .totalIncome(inc)
                    .totalExpense(exp)
                    .netCashFlow(inc.subtract(exp))
                    .build());
        }
        return result;
    }

    // ─── 5. NGÂN SÁCH VS. THỰC TẾ ────────────────────────────────────────────

    /**
     * So sánh hạn mức ngân sách với thực chi cho từng danh mục trong tháng/năm.
     * Chỉ trả về các danh mục đã có budget được tạo cho tháng đó.
     */
    public List<BudgetVsActualResponse> getBudgetVsActual(String email, int month, int year) {
        UUID userId = resolveUserId(email);

        var budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);

        List<BudgetVsActualResponse> result = new ArrayList<>();
        for (var budget : budgets) {
            BigDecimal limit  = budget.getLimit();
            BigDecimal spent  = budgetRepository.calculateSpent(
                    userId, budget.getCategory().getId(), month, year);

            BigDecimal remaining = limit.subtract(spent);

            BigDecimal usagePct = BigDecimal.ZERO;
            if (limit.compareTo(BigDecimal.ZERO) > 0) {
                usagePct = spent.multiply(BigDecimal.valueOf(100))
                                .divide(limit, 2, RoundingMode.HALF_UP);
            }

            result.add(BudgetVsActualResponse.builder()
                    .budgetId(budget.getId())
                    .categoryId(budget.getCategory().getId())
                    .categoryName(budget.getCategory().getName())
                    .budgetLimit(limit)
                    .actualSpent(spent)
                    .remaining(remaining)
                    .usagePercentage(usagePct)
                    .isOverBudget(spent.compareTo(limit) >= 0)
                    .build());
        }
        return result;
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    private UUID resolveUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return user.getId();
    }
}
