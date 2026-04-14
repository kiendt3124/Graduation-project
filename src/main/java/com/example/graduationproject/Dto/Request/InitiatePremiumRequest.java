package com.example.graduationproject.Dto.Request;

import com.example.graduationproject.Entity.Enum.PremiumPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitiatePremiumRequest {

    @NotNull(message = "Vui lòng chọn gói Premium (MONTHLY hoặc YEARLY)")
    private PremiumPlan plan;
}
