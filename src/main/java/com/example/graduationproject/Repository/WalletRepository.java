package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

}
