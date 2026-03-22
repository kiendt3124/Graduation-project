package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.CreateWalletRequest;
import com.example.graduationproject.Dto.Request.DeleteWalletRequest;
import com.example.graduationproject.Dto.Request.UpdateWalletRequest;
import com.example.graduationproject.Dto.Response.WalletResponse;
import com.example.graduationproject.Entity.Wallet;
import com.example.graduationproject.Repository.WalletRepository;
import com.example.graduationproject.Service.WalletService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RequestMapping("/api/wallets")
@RestController
public class WalletController {

    @Autowired
    private WalletService walletService;

    @PostMapping
    public ResponseEntity<?> createWallet(@Valid @RequestBody CreateWalletRequest createWalletRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            WalletResponse wallet = walletService.createWallet(email,createWalletRequest.getBalance(),createWalletRequest.getName(),createWalletRequest.getWalletType());
            return ResponseEntity.ok(wallet);
        }catch (Exception ex){
            return ResponseEntity.badRequest().body("" + ex.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<WalletResponse>> getAllWallets() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<WalletResponse> wallets = walletService.GetListWallet(email);
        return ResponseEntity.ok(wallets);

    }

    @PatchMapping
    public ResponseEntity<?> updateWallet(@Valid @RequestBody UpdateWalletRequest updateWalletRequest) {
        try {
            WalletResponse walletResponse = walletService.updateWallet(updateWalletRequest);
            return ResponseEntity.ok(walletResponse);
        }catch (Exception ex){
            return ResponseEntity.badRequest().body("" + ex.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteWallet(@Valid @RequestBody DeleteWalletRequest deleteWalletRequest) {
        try {
            WalletResponse walletResponse = walletService.deleteWallet(deleteWalletRequest);
            return ResponseEntity.ok(walletResponse);
        }catch (Exception ex){
            return ResponseEntity.badRequest().body("" + ex.getMessage());
        }
    }





}
