package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.CreateWalletRequest;
import com.example.graduationproject.Dto.Response.CreateWalletResponse;
import com.example.graduationproject.Entity.Wallet;
import com.example.graduationproject.Repository.WalletRepository;
import com.example.graduationproject.Service.WalletService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class WalletController {

    @Autowired
    private WalletService walletService;

    @PostMapping("/wallet")
    public ResponseEntity<?> createWallet(@Valid @RequestBody CreateWalletRequest createWalletRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            CreateWalletResponse wallet = walletService.createWallet(email,createWalletRequest.getBalance(),createWalletRequest.getName(),createWalletRequest.getWalletType());
            return ResponseEntity.ok(wallet);
        }catch (Exception ex){
            return ResponseEntity.badRequest().body("" + ex.getMessage());
        }
    }


}
