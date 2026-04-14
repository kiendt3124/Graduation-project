package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.Enum.PremiumOrderStatus;
import com.example.graduationproject.Entity.PremiumOrder;
import com.example.graduationproject.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PremiumOrderRepository extends JpaRepository<PremiumOrder, UUID> {

    Optional<PremiumOrder> findByTxnRef(String txnRef);

    boolean existsByTxnRefAndStatus(String txnRef, PremiumOrderStatus status);

    List<PremiumOrder> findByUserOrderByCreatedAtDesc(User user);
}
