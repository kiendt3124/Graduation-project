package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Response.CreateWalletResponse;
import com.example.graduationproject.Entity.Enum.WalletType;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Entity.Wallet;
import com.example.graduationproject.Repository.UserRepository;
import com.example.graduationproject.Repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public CreateWalletResponse createWallet(String email, BigDecimal balance, String name, WalletType walletType){

        User user = userRepository.findByEmail(email).orElse(null);

        if(user == null) {
            throw new RuntimeException("User not found");
        }else {
            Wallet wallet = Wallet.builder().user(user).balance(balance).name(name).walletType(walletType).build();
            walletRepository.save(wallet);
            return CreateWalletResponse.builder().id(wallet.getId()).name(wallet.getName()).balance(wallet.getBalance()).walletType(wallet.getWalletType()).build();
        }
    }


}
