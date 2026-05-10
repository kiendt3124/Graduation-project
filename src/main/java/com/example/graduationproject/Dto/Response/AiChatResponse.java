package com.example.graduationproject.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatResponse {

    /** ID phiên hội thoại (dùng cho các request tiếp theo) */
    private UUID sessionId;

    /** Câu trả lời của AI (text) */
    private String reply;

    /**
     * Nếu AI detect yêu cầu tạo giao dịch và thực hiện thành công,
     * field này chứa giao dịch vừa tạo. Null nếu không có action.
     */
    private TransactionResponse createdTransaction;

    /** Số tin nhắn đã dùng hôm nay (để hiển thị remaining quota) */
    private int usedToday;

    /** Giới hạn tin nhắn/ngày */
    private int dailyLimit;
}
