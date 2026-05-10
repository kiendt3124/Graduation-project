package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Request.FinancialGoalRequest;
import com.example.graduationproject.Dto.Response.FinancialGoalResponse;
import com.example.graduationproject.Dto.Response.GoalContributionResponse;
import com.example.graduationproject.Entity.Enum.GoalStatus;
import com.example.graduationproject.Entity.FinancialGoal;
import com.example.graduationproject.Entity.GoalContribution;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Entity.Wallet;
import com.example.graduationproject.Repository.FinancialGoalRepository;
import com.example.graduationproject.Repository.GoalContributionRepository;
import com.example.graduationproject.Repository.UserRepository;
import com.example.graduationproject.Repository.WalletRepository;
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
public class FinancialGoalService {

    private final FinancialGoalRepository goalRepository;
    private final GoalContributionRepository contributionRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Transactional
    public FinancialGoalResponse createGoal(String email, FinancialGoalRequest.Create req) {
        User user = getUserByEmail(email);

        FinancialGoal goal = FinancialGoal.builder()
                .user(user)
                .name(req.getName())
                .targetAmount(req.getTargetAmount())
                .deadline(req.getDeadline())
                .note(req.getNote())
                .build();

        goalRepository.save(goal);
        return toResponse(goal);
    }

    // ─── GET LIST ─────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách mục tiêu, có thể filter theo status.
     * Thứ tự: ACTIVE → COMPLETED → CANCELLED, deadline gần nhất lên trước.
     */
    @Transactional(readOnly = true)
    public List<FinancialGoalResponse> getGoals(String email, GoalStatus status) {
        User user = getUserByEmail(email);

        List<FinancialGoal> goals;
        if (status != null) {
            goals = goalRepository.findByUserIdAndStatusAndIsDeletedFalse(user.getId(), status);
        } else {
            goals = goalRepository.findByUserIdAndIsDeletedFalse(user.getId());
        }

        // Sắp xếp trên entity trước khi map — tránh unchecked cast trong lambda
        goals.sort(Comparator
                .comparingInt((FinancialGoal g) -> statusOrder(g.getStatus()))
                .thenComparing(FinancialGoal::getDeadline,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        return goals.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── GET BY ID ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FinancialGoalResponse getGoalById(String email, UUID goalId) {
        User user = getUserByEmail(email);
        FinancialGoal goal = getGoalAndVerifyOwner(goalId, user.getId());
        return toResponse(goal);
    }

    // ─── UPDATE (partial patch) ───────────────────────────────────────────────

    @Transactional
    public FinancialGoalResponse updateGoal(String email, UUID goalId, FinancialGoalRequest.Update req) {
        User user = getUserByEmail(email);
        FinancialGoal goal = getGoalAndVerifyOwner(goalId, user.getId());

        if (goal.getStatus() == GoalStatus.CANCELLED) {
            throw new RuntimeException("Mục tiêu đã bị huỷ, không thể cập nhật");
        }

        if (req.getName() != null)         goal.setName(req.getName());
        if (req.getTargetAmount() != null) {
            goal.setTargetAmount(req.getTargetAmount());
            // Kiểm tra lại: nếu cập nhật target xuống thấp hơn số tiền đã góp -> tự động COMPLETED
            if (goal.getStatus() == GoalStatus.ACTIVE
                    && goal.getCurrentAmount().compareTo(req.getTargetAmount()) >= 0) {
                goal.setStatus(GoalStatus.COMPLETED);
            }
        }
        if (req.getDeadline() != null)     goal.setDeadline(req.getDeadline());
        if (req.getNote() != null)         goal.setNote(req.getNote());

        // Chỉ cho phép đổi sang CANCELLED thủ công; COMPLETED do hệ thống quản lý
        if (req.getStatus() == GoalStatus.CANCELLED) {
            goal.setStatus(GoalStatus.CANCELLED);
        }

        goalRepository.save(goal);
        return toResponse(goal);
    }

    // ─── DELETE (soft) ───────────────────────────────────────────────────────

    @Transactional
    public void deleteGoal(String email, UUID goalId) {
        User user = getUserByEmail(email);
        FinancialGoal goal = getGoalAndVerifyOwner(goalId, user.getId());
        goal.setIsDeleted(true);
        goalRepository.save(goal);
    }

    // ─── ADD CONTRIBUTION ────────────────────────────────────────────────────

    @Transactional
    public GoalContributionResponse addContribution(
            String email, UUID goalId, FinancialGoalRequest.AddContribution req) {

        User user = getUserByEmail(email);
        FinancialGoal goal = getGoalAndVerifyOwner(goalId, user.getId());

        if (goal.getStatus() == GoalStatus.COMPLETED) {
            throw new RuntimeException("Mục tiêu đã hoàn thành, không thể góp thêm");
        }
        if (goal.getStatus() == GoalStatus.CANCELLED) {
            throw new RuntimeException("Mục tiêu đã bị huỷ, không thể góp tiền");
        }

        // ── Validate ví ──────────────────────────────────────────────────────
        Wallet wallet = walletRepository.findById(req.getWalletId())
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        if (wallet.getIsDeleted()) {
            throw new RuntimeException("Ví đã bị xoá");
        }
        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Ví không thuộc về bạn");
        }

        // ── Kiểm tra số dư ví ────────────────────────────────────────────────
        if (wallet.getBalance().compareTo(req.getAmount()) < 0) {
            throw new RuntimeException(
                    "Số dư ví không đủ. Hiện có: " + wallet.getBalance() +
                    ", cần: " + req.getAmount());
        }

        // ── Trừ số dư ví ────────────────────────────────────────────────────
        wallet.setBalance(wallet.getBalance().subtract(req.getAmount()));
        walletRepository.save(wallet);

        // ── Lưu contribution ────────────────────────────────────────────────
        GoalContribution contribution = GoalContribution.builder()
                .goal(goal)
                .wallet(wallet)
                .amount(req.getAmount())
                .contributionDate(req.getContributionDate())
                .note(req.getNote())
                .build();
        contributionRepository.save(contribution);

        // ── Cập nhật currentAmount ───────────────────────────────────────────
        BigDecimal newCurrent = contributionRepository.sumContributedAmountByGoalId(goalId);
        goal.setCurrentAmount(newCurrent);

        // ── Auto COMPLETED nếu đạt target ───────────────────────────────────
        if (newCurrent.compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
        }
        goalRepository.save(goal);

        return toContributionResponse(contribution);
    }

    // ─── GET CONTRIBUTIONS ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GoalContributionResponse> getContributions(String email, UUID goalId) {
        User user = getUserByEmail(email);
        getGoalAndVerifyOwner(goalId, user.getId()); // verify ownership

        return contributionRepository.findByGoalIdOrderByContributionDateDesc(goalId)
                .stream()
                .map(this::toContributionResponse)
                .collect(Collectors.toList());
    }

    // ─── DELETE CONTRIBUTION ─────────────────────────────────────────────────

    @Transactional
    public void deleteContribution(String email, UUID goalId, UUID contributionId) {
        User user = getUserByEmail(email);
        FinancialGoal goal = getGoalAndVerifyOwner(goalId, user.getId());

        GoalContribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new RuntimeException("Lần góp tiền không tồn tại"));

        if (!contribution.getGoal().getId().equals(goalId)) {
            throw new RuntimeException("Lần góp tiền không thuộc mục tiêu này");
        }

        // ── Hoàn tiền về ví ──────────────────────────────────────────────────
        Wallet wallet = contribution.getWallet();
        wallet.setBalance(wallet.getBalance().add(contribution.getAmount()));
        walletRepository.save(wallet);

        // ── Xoá contribution ─────────────────────────────────────────────────
        contributionRepository.delete(contribution);

        // ── Tính lại currentAmount ────────────────────────────────────────────
        BigDecimal newCurrent = contributionRepository.sumContributedAmountByGoalId(goalId);
        goal.setCurrentAmount(newCurrent);

        // ── Reset status về ACTIVE nếu amount giảm xuống dưới target ─────────
        if (goal.getStatus() == GoalStatus.COMPLETED
                && newCurrent.compareTo(goal.getTargetAmount()) < 0) {
            goal.setStatus(GoalStatus.ACTIVE);
        }
        goalRepository.save(goal);
    }

    // ─── HELPER: Entity → Response ────────────────────────────────────────────

    private FinancialGoalResponse toResponse(FinancialGoal goal) {
        BigDecimal target  = goal.getTargetAmount();
        BigDecimal current = goal.getCurrentAmount();

        BigDecimal remaining = target.subtract(current).max(BigDecimal.ZERO);

        BigDecimal progressPercent;
        if (target.compareTo(BigDecimal.ZERO) == 0) {
            progressPercent = BigDecimal.valueOf(100);
        } else {
            progressPercent = current
                    .multiply(BigDecimal.valueOf(100))
                    .divide(target, 2, RoundingMode.HALF_UP)
                    .min(BigDecimal.valueOf(100));
        }

        boolean isOverdue = goal.getDeadline() != null
                && LocalDate.now().isAfter(goal.getDeadline())
                && goal.getStatus() != GoalStatus.COMPLETED;

        return FinancialGoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(target)
                .currentAmount(current)
                .remainingAmount(remaining)
                .progressPercent(progressPercent)
                .status(goal.getStatus())
                .isOverdue(isOverdue)
                .deadline(goal.getDeadline())
                .note(goal.getNote())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }

    private GoalContributionResponse toContributionResponse(GoalContribution c) {
        return GoalContributionResponse.builder()
                .id(c.getId())
                .goalId(c.getGoal().getId())
                .walletId(c.getWallet().getId())
                .walletName(c.getWallet().getName())
                .amount(c.getAmount())
                .contributionDate(c.getContributionDate())
                .note(c.getNote())
                .createdAt(c.getCreatedAt())
                .build();
    }

    // ─── HELPER: ownership & status order ────────────────────────────────────

    private FinancialGoal getGoalAndVerifyOwner(UUID goalId, UUID userId) {
        FinancialGoal goal = goalRepository.findByIdAndIsDeletedFalse(goalId)
                .orElseThrow(() -> new RuntimeException("Mục tiêu không tồn tại"));
        if (!goal.getUser().getId().equals(userId)) {
            throw new RuntimeException("Không có quyền truy cập mục tiêu này");
        }
        return goal;
    }

    /** Thứ tự ưu tiên: ACTIVE(0) → COMPLETED(1) → CANCELLED(2) */
    private int statusOrder(GoalStatus status) {
        return switch (status) {
            case ACTIVE    -> 0;
            case COMPLETED -> 1;
            case CANCELLED -> 2;
        };
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
