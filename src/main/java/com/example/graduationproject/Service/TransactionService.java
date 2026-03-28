package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Request.CreateTransactionRequest;
import com.example.graduationproject.Dto.Request.DeleteTransactionRequest;
import com.example.graduationproject.Dto.Request.UpdateTransactionRequest;
import com.example.graduationproject.Dto.Response.TransactionResponse;
import com.example.graduationproject.Entity.Category;
import com.example.graduationproject.Entity.Enum.TransactionType;
import com.example.graduationproject.Entity.Transaction;
import com.example.graduationproject.Entity.Wallet;
import com.example.graduationproject.Repository.CategoryRepository;
import com.example.graduationproject.Repository.TransactionRepository;
import com.example.graduationproject.Repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest req) {

        Wallet fromWallet = walletRepository.findById(req.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        Transaction transaction;

        if (TransactionType.TRANSFER.equals(req.getTransactionType())) {
            // TRANSFER: toWalletId bắt buộc, categoryId không cần
            if (req.getToWalletId() == null) {
                throw new RuntimeException("toWalletId is required for TRANSFER");
            }
            Wallet toWallet = walletRepository.findById(req.getToWalletId())
                    .orElseThrow(() -> new RuntimeException("Destination wallet not found"));

            if (req.getAmount().compareTo(fromWallet.getBalance()) > 0) {
                throw new RuntimeException("Insufficient balance");
            }

            fromWallet.setBalance(fromWallet.getBalance().subtract(req.getAmount()));
            toWallet.setBalance(toWallet.getBalance().add(req.getAmount()));
            walletRepository.save(fromWallet);
            walletRepository.save(toWallet);

            transaction = Transaction.builder()
                    .wallet(fromWallet)
                    .toWallet(toWallet)
                    .amount(req.getAmount())
                    .transactionType(req.getTransactionType())
                    .transactionDate(req.getTransactionDate())
                    .note(req.getNote())
                    .build();

        } else {
            // INCOME / EXPENSE: categoryId bắt buộc
            if (req.getCategoryId() == null) {
                throw new RuntimeException("categoryId is required for INCOME/EXPENSE");
            }
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            if (TransactionType.EXPENSE.equals(req.getTransactionType())) {
                if (req.getAmount().compareTo(fromWallet.getBalance()) > 0) {
                    throw new RuntimeException("Insufficient balance");
                }
                fromWallet.setBalance(fromWallet.getBalance().subtract(req.getAmount()));
            } else {
                // INCOME
                fromWallet.setBalance(fromWallet.getBalance().add(req.getAmount()));
            }
            walletRepository.save(fromWallet);

            transaction = Transaction.builder()
                    .wallet(fromWallet)
                    .category(category)
                    .amount(req.getAmount())
                    .transactionType(req.getTransactionType())
                    .transactionDate(req.getTransactionDate())
                    .note(req.getNote())
                    .build();
        }

        transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    // ─── GET LIST (lấy tất cả transaction của 1 wallet) ──────────────────────

    public List<TransactionResponse> getTransactionsByWallet(String email, java.util.UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // Kiểm tra wallet có thuộc user đang đăng nhập không
        if (!wallet.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Access denied");
        }

        return wallet.getTransactions().stream()
                .filter(t -> !t.getIsDeleted())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse updateTransaction(UpdateTransactionRequest req) {
        Transaction transaction = transactionRepository.findById(req.getId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.getIsDeleted()) {
            throw new RuntimeException("Transaction has been deleted");
        }

        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            transaction.setCategory(category);
        }

        if (req.getAmount() != null) {
            // Hoàn lại số tiền cũ vào ví rồi trừ/cộng số tiền mới
            Wallet wallet = transaction.getWallet();
            if (TransactionType.EXPENSE.equals(transaction.getTransactionType())) {
                wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));      // hoàn lại
                if (req.getAmount().compareTo(wallet.getBalance()) > 0) {
                    throw new RuntimeException("Insufficient balance");
                }
                wallet.setBalance(wallet.getBalance().subtract(req.getAmount()));        // trừ mới
            } else if (TransactionType.INCOME.equals(transaction.getTransactionType())) {
                wallet.setBalance(wallet.getBalance().subtract(transaction.getAmount())); // hoàn lại
                wallet.setBalance(wallet.getBalance().add(req.getAmount()));              // cộng mới
            }
            walletRepository.save(wallet);
            transaction.setAmount(req.getAmount());
        }

        if (req.getTransactionDate() != null) {
            transaction.setTransactionDate(req.getTransactionDate());
        }

        if (req.getNote() != null) {
            transaction.setNote(req.getNote());
        }

        transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    // ─── DELETE (soft delete) ─────────────────────────────────────────────────

    @Transactional
    public TransactionResponse deleteTransaction(DeleteTransactionRequest req) {
        Transaction transaction = transactionRepository.findById(req.getId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.getIsDeleted()) {
            throw new RuntimeException("Transaction already deleted");
        }

        // Hoàn lại số tiền về ví
        Wallet wallet = transaction.getWallet();
        if (TransactionType.EXPENSE.equals(transaction.getTransactionType())) {
            wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
        } else if (TransactionType.INCOME.equals(transaction.getTransactionType())) {
            wallet.setBalance(wallet.getBalance().subtract(transaction.getAmount()));
        } else if (TransactionType.TRANSFER.equals(transaction.getTransactionType())) {
            // Hoàn lại: cộng lại ví nguồn, trừ ví đích
            wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
            Wallet toWallet = transaction.getToWallet();
            toWallet.setBalance(toWallet.getBalance().subtract(transaction.getAmount()));
            walletRepository.save(toWallet);
        }
        walletRepository.save(wallet);

        transaction.setIsDeleted(true);
        transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    // ─── HELPER: Entity → Response ────────────────────────────────────────────

    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .walletId(t.getWallet().getId())
                .walletName(t.getWallet().getName())
                .toWalletId(t.getToWallet() != null ? t.getToWallet().getId() : null)
                .toWalletName(t.getToWallet() != null ? t.getToWallet().getName() : null)
                .categoryId(t.getCategory() != null ? t.getCategory().getId() : null)
                .categoryName(t.getCategory() != null ? t.getCategory().getName() : null)
                .amount(t.getAmount())
                .transactionType(t.getTransactionType())
                .transactionDate(t.getTransactionDate())
                .note(t.getNote())
                .isDeleted(t.getIsDeleted())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
