package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Request.LoanRequest;
import com.example.graduationproject.Dto.Response.LoanPaymentResponse;
import com.example.graduationproject.Dto.Response.LoanResponse;
import com.example.graduationproject.Entity.Enum.LoanStatus;
import com.example.graduationproject.Entity.Enum.LoanType;
import com.example.graduationproject.Entity.Loan;
import com.example.graduationproject.Entity.LoanPayment;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Entity.Wallet;
import com.example.graduationproject.Repository.LoanPaymentRepository;
import com.example.graduationproject.Repository.LoanRepository;
import com.example.graduationproject.Repository.UserRepository;
import com.example.graduationproject.Repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Transactional
    public LoanResponse createLoan(String email, LoanRequest.Create req) {
        User user = getUserByEmail(email);

        Loan loan = Loan.builder()
                .user(user)
                .counterpart(req.getCounterpart())
                .loanType(req.getLoanType())
                .principalAmount(req.getPrincipalAmount())
                .interestRate(req.getInterestRate())
                .startDate(req.getStartDate())
                .dueDate(req.getDueDate())
                .note(req.getNote())
                .status(LoanStatus.ACTIVE)
                .build();

        loanRepository.save(loan);
        return toResponse(loan);
    }

    // ─── GET LIST ─────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách loan với filter tuỳ chọn theo type và/hoặc status.
     * Tự động cập nhật OVERDUE khi lấy danh sách.
     */
    @Transactional
    public List<LoanResponse> getLoans(String email, LoanType loanType, LoanStatus status) {
        User user = getUserByEmail(email);

        List<Loan> loans;

        if (loanType != null && status != null) {
            loans = loanRepository.findByUserIdAndLoanTypeAndStatusAndIsDeletedFalse(
                    user.getId(), loanType, status);
        } else if (loanType != null) {
            loans = loanRepository.findByUserIdAndLoanTypeAndIsDeletedFalse(user.getId(), loanType);
        } else if (status != null) {
            loans = loanRepository.findByUserIdAndStatusAndIsDeletedFalse(user.getId(), status);
        } else {
            loans = loanRepository.findByUserIdAndIsDeletedFalse(user.getId());
        }

        // Tự động đánh dấu OVERDUE nếu chưa PAID và đã quá dueDate
        loans.forEach(loan -> {
            if (loan.getStatus() == LoanStatus.ACTIVE
                    && loan.getDueDate() != null
                    && LocalDate.now().isAfter(loan.getDueDate())) {
                loan.setStatus(LoanStatus.OVERDUE);
                loanRepository.save(loan);
            }
        });

        return loans.stream()
                .map(this::toResponse)
                .sorted(Comparator
                        // OVERDUE lên trước, sau đó ACTIVE, cuối là PAID
                        .comparingInt(r -> statusOrder(((LoanResponse) r).getStatus()))
                        // Trong cùng status: hạn gần nhất lên trước (null xuống cuối)
                        .thenComparing(r -> ((LoanResponse) r).getDueDate(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // ─── GET BY ID ────────────────────────────────────────────────────────────

    public LoanResponse getLoanById(String email, UUID loanId) {
        User user = getUserByEmail(email);
        Loan loan = getLoanAndVerifyOwner(loanId, user.getId());
        return toResponse(loan);
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @Transactional
    public LoanResponse updateLoan(String email, UUID loanId, LoanRequest.Update req) {
        User user = getUserByEmail(email);
        Loan loan = getLoanAndVerifyOwner(loanId, user.getId());

        // Patch: chỉ cập nhật field != null
        if (req.getCounterpart() != null) {
            loan.setCounterpart(req.getCounterpart());
        }
        if (req.getInterestRate() != null) {
            loan.setInterestRate(req.getInterestRate());
        }
        if (req.getDueDate() != null) {
            loan.setDueDate(req.getDueDate());
        }
        if (req.getStatus() != null) {
            loan.setStatus(req.getStatus());
        }
        if (req.getNote() != null) {
            loan.setNote(req.getNote());
        }

        loanRepository.save(loan);
        return toResponse(loan);
    }

    // ─── DELETE (soft) ───────────────────────────────────────────────────────

    @Transactional
    public void deleteLoan(String email, UUID loanId) {
        User user = getUserByEmail(email);
        Loan loan = getLoanAndVerifyOwner(loanId, user.getId());
        loan.setIsDeleted(true);
        loanRepository.save(loan);
    }

    // ─── ADD PAYMENT ─────────────────────────────────────────────────────────

    @Transactional
    public LoanPaymentResponse addPayment(String email, UUID loanId, LoanRequest.AddPayment req) {
        User user = getUserByEmail(email);
        Loan loan = getLoanAndVerifyOwner(loanId, user.getId());

        if (loan.getStatus() == LoanStatus.PAID) {
            throw new RuntimeException("Khoản vay này đã được thanh toán đầy đủ");
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

        // ── Tính số tiền thực tế cần trả ────────────────────────────────────
        BigDecimal totalDue = calculateTotalDue(loan, LocalDate.now());
        BigDecimal paidSoFar = loanPaymentRepository.sumPaidAmountByLoanId(loanId);
        BigDecimal remaining = totalDue.subtract(paidSoFar);

        // Nếu trả vượt quá số còn lại → tự động cap về đúng remaining
        BigDecimal actualAmount = req.getAmount().compareTo(remaining) > 0
                ? remaining
                : req.getAmount();

        // ── Kiểm tra số dư ví ───────────────────────────────────────────────
        if (wallet.getBalance().compareTo(actualAmount) < 0) {
            throw new RuntimeException(
                    "Số dư ví không đủ. Hiện có: " + wallet.getBalance() +
                    ", cần: " + actualAmount);
        }

        // ── Trừ số dư ví ────────────────────────────────────────────────────
        wallet.setBalance(wallet.getBalance().subtract(actualAmount));
        walletRepository.save(wallet);

        // ── Lưu payment ─────────────────────────────────────────────────────
        LoanPayment payment = LoanPayment.builder()
                .loan(loan)
                .wallet(wallet)
                .amount(actualAmount)
                .paymentDate(req.getPaymentDate())
                .note(req.getNote())
                .build();

        loanPaymentRepository.save(payment);

        // ── Nếu đã trả đủ → chuyển sang PAID ────────────────────────────────
        BigDecimal newPaid = paidSoFar.add(actualAmount);
        if (newPaid.compareTo(totalDue) >= 0) {
            loan.setStatus(LoanStatus.PAID);
            loanRepository.save(loan);
        }

        return toPaymentResponse(payment);
    }

    // ─── GET PAYMENTS ─────────────────────────────────────────────────────────

    public List<LoanPaymentResponse> getPayments(String email, UUID loanId) {
        User user = getUserByEmail(email);
        getLoanAndVerifyOwner(loanId, user.getId()); // verify ownership

        return loanPaymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId)
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    // ─── HELPER: tính lãi đơn ─────────────────────────────────────────────────

    /**
     * Lãi đơn tích lũy từ startDate đến today.
     * interestAmount = principal × rate/100 × days / 365
     * Trả về 0 nếu interestRate == null hoặc rate == 0.
     */
    private BigDecimal calculateAccruedInterest(Loan loan, LocalDate today) {
        if (loan.getInterestRate() == null
                || loan.getInterestRate().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        long days = ChronoUnit.DAYS.between(loan.getStartDate(), today);
        if (days <= 0) return BigDecimal.ZERO;

        return loan.getPrincipalAmount()
                .multiply(loan.getInterestRate())
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(days))
                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
    }

    /**
     * Tổng tiền phải trả = gốc + lãi tích lũy tính đến today.
     */
    private BigDecimal calculateTotalDue(Loan loan, LocalDate today) {
        return loan.getPrincipalAmount().add(calculateAccruedInterest(loan, today));
    }

    // ─── HELPER: Entity → Response ────────────────────────────────────────────

    private LoanResponse toResponse(Loan loan) {
        LocalDate today = LocalDate.now();

        BigDecimal interestAmount = calculateAccruedInterest(loan, today);
        BigDecimal totalDue = loan.getPrincipalAmount().add(interestAmount);
        BigDecimal paidAmount = loanPaymentRepository.sumPaidAmountByLoanId(loan.getId());
        BigDecimal remainingAmount = totalDue.subtract(paidAmount);

        BigDecimal progressPercent;
        if (totalDue.compareTo(BigDecimal.ZERO) == 0) {
            progressPercent = BigDecimal.valueOf(100);
        } else {
            progressPercent = paidAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalDue, 2, RoundingMode.HALF_UP);
        }

        boolean isOverdue = loan.getDueDate() != null
                && today.isAfter(loan.getDueDate())
                && loan.getStatus() != LoanStatus.PAID;

        return LoanResponse.builder()
                .id(loan.getId())
                .counterpart(loan.getCounterpart())
                .loanType(loan.getLoanType())
                .status(loan.getStatus())
                .principalAmount(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .interestAmount(interestAmount)
                .totalDue(totalDue)
                .paidAmount(paidAmount)
                .remainingAmount(remainingAmount.max(BigDecimal.ZERO))
                .progressPercent(progressPercent)
                .startDate(loan.getStartDate())
                .dueDate(loan.getDueDate())
                .isOverdue(isOverdue)
                .note(loan.getNote())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }

    private LoanPaymentResponse toPaymentResponse(LoanPayment payment) {
        return LoanPaymentResponse.builder()
                .id(payment.getId())
                .loanId(payment.getLoan().getId())
                .walletId(payment.getWallet().getId())
                .walletName(payment.getWallet().getName())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .note(payment.getNote())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    // ─── HELPER: ownership & status order ────────────────────────────────────

    private Loan getLoanAndVerifyOwner(UUID loanId, UUID userId) {
        Loan loan = loanRepository.findByIdAndIsDeletedFalse(loanId)
                .orElseThrow(() -> new RuntimeException("Khoản vay không tồn tại"));
        if (!loan.getUser().getId().equals(userId)) {
            throw new RuntimeException("Không có quyền truy cập khoản vay này");
        }
        return loan;
    }

    /** Thứ tự ưu tiên hiển thị: OVERDUE(0) → ACTIVE(1) → PAID(2) */
    private int statusOrder(LoanStatus status) {
        return switch (status) {
            case OVERDUE -> 0;
            case ACTIVE  -> 1;
            case PAID    -> 2;
        };
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
