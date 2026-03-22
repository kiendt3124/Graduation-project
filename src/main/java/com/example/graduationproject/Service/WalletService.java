package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Request.DeleteWalletRequest;
import com.example.graduationproject.Dto.Request.UpdateWalletRequest;
import com.example.graduationproject.Dto.Response.WalletResponse;
import com.example.graduationproject.Entity.Enum.WalletType;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Entity.Wallet;
import com.example.graduationproject.Repository.UserRepository;
import com.example.graduationproject.Repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public WalletResponse createWallet(String email, BigDecimal balance, String name, WalletType walletType){

        User user = userRepository.findByEmail(email).orElse(null);

        if(user == null) {
            throw new RuntimeException("User not found");
        }else {
            Wallet wallet = Wallet.builder()
                    .user(user)
                    .balance(balance)
                    .name(name)
                    .walletType(walletType)
                    .isDeleted(false)
                    .build();
            walletRepository.save(wallet);
            return WalletResponse.builder()
                    .id(wallet.getId())
                    .name(wallet.getName())
                    .balance(wallet.getBalance())
                    .walletType(wallet.getWalletType())
                    .isDeleted(wallet.getIsDeleted())
                    .build();
        }
    }

    public List<WalletResponse> GetListWallet(String email){
        User user = userRepository.findByEmail(email).orElse(null);

        if(user == null) {
            throw new RuntimeException("User not found");
        }else  {
            List<Wallet> list = user.getWallets();
            List<WalletResponse> walletResponses = new ArrayList<>();
            for (Wallet wallet : list) {
                WalletResponse walletResponse = WalletResponse.builder()
                        .id(wallet.getId())
                        .name(wallet.getName())
                        .balance(wallet.getBalance())
                        .walletType(wallet.getWalletType())
                        .isDeleted(wallet.getIsDeleted())
                        .build();
                walletResponses.add(walletResponse);
            }
            return walletResponses;
        }

    }

    public WalletResponse updateWallet(UpdateWalletRequest  updateWalletRequest){
        Wallet wallet = walletRepository.findById(updateWalletRequest.getId()).orElse(null);
        if(wallet == null) {
            throw new RuntimeException("Wallet not found");
        }else  {
            if (updateWalletRequest.getName() != null) {
                wallet.setName(updateWalletRequest.getName());
            }
            if (updateWalletRequest.getBalance() != null) {
                wallet.setBalance(updateWalletRequest.getBalance());
            }
            if (updateWalletRequest.getWalletType() != null) {
                wallet.setWalletType(updateWalletRequest.getWalletType());
            }
            if (updateWalletRequest.getIsDeleted() != null) {
                wallet.setIsDeleted(updateWalletRequest.getIsDeleted());
            }
            walletRepository.save(wallet);
            return WalletResponse.builder()
                    .id(wallet.getId())
                    .name(wallet.getName())
                    .balance(wallet.getBalance())
                    .walletType(wallet.getWalletType())
                    .isDeleted(wallet.getIsDeleted())
                    .build();
        }
    }

    public WalletResponse deleteWallet(DeleteWalletRequest deleteWalletRequest){
        Wallet wallet = walletRepository.findById(deleteWalletRequest.getId()).orElse(null);
        if(wallet == null) {
            throw new RuntimeException("Wallet not found");
        }else{
            wallet.setIsDeleted(true);
            walletRepository.save(wallet);
            return WalletResponse.builder()
                    .id(wallet.getId())
                    .name(wallet.getName())
                    .balance(wallet.getBalance())
                    .walletType(wallet.getWalletType())
                    .isDeleted(wallet.getIsDeleted())
                    .build();
        }
    }











}
