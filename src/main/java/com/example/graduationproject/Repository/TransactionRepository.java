package com.example.graduationproject.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.graduationproject.Entity.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

}
