package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.Category;
import com.example.graduationproject.Entity.Enum.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByTransactionType(TransactionType transactionType);
}
