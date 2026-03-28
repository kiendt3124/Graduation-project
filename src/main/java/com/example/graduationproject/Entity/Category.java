package com.example.graduationproject.Entity;

import com.example.graduationproject.Entity.Enum.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    /**
     * Icon identifier (e.g. emoji or icon name from frontend icon library)
     * Example: "🍔", "car", "salary"
     */
    @Column
    private String icon;

    /**
     * Determines whether this category is for INCOME or EXPENSE transactions.
     * TRANSFER categories are handled separately by the wallet transfer feature.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Builder.Default
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Budget> budgets = new ArrayList<>();
}
