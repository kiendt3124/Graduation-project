package com.example.graduationproject.Dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class AiChatRequest {

    @NotBlank(message = "Tin nhắn không được để trống")
    private String message;

    /**
     * ID phiên hội thoại.
     * null = tạo session mới, có giá trị = tiếp tục conversation cũ.
     */
    private UUID sessionId;
}
