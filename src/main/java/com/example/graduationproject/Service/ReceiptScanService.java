package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Response.ScanReceiptResponse;
import com.example.graduationproject.Entity.Category;
import com.example.graduationproject.Entity.Enum.AccountTier;
import com.example.graduationproject.Entity.Enum.TransactionType;
import com.example.graduationproject.Entity.User;
import com.example.graduationproject.Repository.CategoryRepository;
import com.example.graduationproject.Repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service scan hóa đơn bằng AI (Gemini Vision).
 * Flow: Upload Cloudinary → Gemini đọc ảnh → trích xuất JSON → match category.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptScanService {

    private final ImageUploadService imageUploadService;
    private final GeminiClient geminiClient;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Bạn là ReceiptBot — trợ lý AI chuyên đọc hóa đơn và biên lai.
            Nhiệm vụ: Đọc ảnh hóa đơn/biên lai và trích xuất thông tin giao dịch.
            
            Quy tắc:
            1. Trả kết quả dưới dạng JSON duy nhất, KHÔNG giải thích gì thêm
            2. Nếu có nhiều mặt hàng, tính TỔNG amount
            3. Nếu không đọc được thông tin nào, để giá trị null
            4. amount luôn là số nguyên (đơn vị VND)
            5. transactionType hầu hết là "EXPENSE"
            6. suggestedCategory phải là 1 trong: Ăn uống, Mua sắm, Di chuyển, Giải trí, Hóa đơn & Tiện ích, Sức khỏe, Giáo dục, Khác
            """;

    private static final String TEXT_PROMPT = """
            Đọc ảnh hóa đơn/biên lai này và trả về JSON:
            {
              "amount": <số tiền tổng (số nguyên, VND)>,
              "transactionType": "EXPENSE",
              "merchantName": "<tên cửa hàng/nơi bán>",
              "suggestedCategory": "<gợi ý danh mục>",
              "note": "<mô tả ngắn gọn nội dung hóa đơn>",
              "transactionDate": "<ngày trên hóa đơn, format: yyyy-MM-ddTHH:mm:ss>"
            }
            Chỉ trả JSON, không giải thích.
            """;

    /**
     * Scan hóa đơn: upload ảnh + AI đọc + trả kết quả.
     */
    public ScanReceiptResponse scanReceipt(String email, MultipartFile file) {
        // 1. Kiểm tra Premium
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));

        if (user.getAccountTier() != AccountTier.PREMIUM) {
            throw new RuntimeException("PREMIUM_REQUIRED");
        }

        // 2. Upload ảnh lên Cloudinary
        String imageUrl = imageUploadService.uploadImage(file);
        log.info("[ReceiptScan] Ảnh uploaded: {}", imageUrl);

        // 3. Gửi ảnh tới Gemini Vision
        byte[] imageBytes;
        String mimeType;
        try {
            imageBytes = file.getBytes();
            mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file ảnh: " + e.getMessage(), e);
        }

        GeminiClient.GeminiResult result = geminiClient.analyzeImage(
                SYSTEM_PROMPT, TEXT_PROMPT, imageBytes, mimeType);

        log.info("[ReceiptScan] AI response: {}", result.text());

        // 4. Parse JSON từ AI
        return parseAiResponse(result.text(), imageUrl);
    }

    /**
     * Parse JSON response từ AI thành ScanReceiptResponse.
     */
    private ScanReceiptResponse parseAiResponse(String aiText, String imageUrl) {
        try {
            // Trích xuất JSON từ response (AI có thể wrap trong ```json ... ```)
            String jsonStr = extractJson(aiText);
            JsonNode json = objectMapper.readTree(jsonStr);

            // Parse fields
            BigDecimal amount = null;
            if (json.has("amount") && !json.get("amount").isNull()) {
                amount = new BigDecimal(json.get("amount").asText("0"));
            }

            String transactionType = json.path("transactionType").asText("EXPENSE");
            String merchantName = json.path("merchantName").asText(null);
            String suggestedCategory = json.path("suggestedCategory").asText(null);
            String note = json.path("note").asText(null);
            String transactionDate = json.path("transactionDate").asText(null);

            // Nếu AI trả merchantName, thêm vào note
            if (merchantName != null && !merchantName.isBlank()) {
                note = (note != null ? note : "") + " — " + merchantName;
                note = note.trim();
                if (note.startsWith("—")) note = note.substring(1).trim();
            }

            // Match category từ DB
            java.util.UUID categoryId = matchCategory(suggestedCategory, transactionType);

            return ScanReceiptResponse.builder()
                    .amount(amount)
                    .transactionType(transactionType)
                    .suggestedCategoryName(suggestedCategory)
                    .suggestedCategoryId(categoryId)
                    .merchantName(merchantName)
                    .note(note)
                    .transactionDate(transactionDate)
                    .imageUrl(imageUrl)
                    .build();

        } catch (Exception e) {
            log.warn("[ReceiptScan] Không parse được AI response: {}", e.getMessage());
            // Trả về response tối thiểu — có imageUrl, user tự nhập phần còn lại
            return ScanReceiptResponse.builder()
                    .imageUrl(imageUrl)
                    .transactionType("EXPENSE")
                    .note("AI không đọc được hóa đơn — vui lòng nhập thủ công")
                    .build();
        }
    }

    /**
     * Trích xuất JSON từ text (AI có thể wrap trong markdown code block).
     */
    private String extractJson(String text) {
        if (text == null) return "{}";

        // Tìm ```json ... ``` hoặc ``` ... ```
        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7;
            int end = text.indexOf("```", start);
            if (end > start) return text.substring(start, end).trim();
        }
        if (text.contains("```")) {
            int start = text.indexOf("```") + 3;
            int end = text.indexOf("```", start);
            if (end > start) return text.substring(start, end).trim();
        }

        // Tìm { ... } trực tiếp
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return text.trim();
    }

    /**
     * Match tên category gợi ý từ AI với category trong DB.
     * Tìm theo tên gần đúng (contains, case-insensitive).
     */
    private java.util.UUID matchCategory(String suggestedCategory, String transactionType) {
        if (suggestedCategory == null || suggestedCategory.isBlank()) return null;

        try {
            TransactionType type = "INCOME".equalsIgnoreCase(transactionType)
                    ? TransactionType.INCOME : TransactionType.EXPENSE;

            List<Category> categories = categoryRepository.findByTransactionType(type);

            // Tìm exact match trước
            Optional<Category> exact = categories.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(suggestedCategory))
                    .findFirst();
            if (exact.isPresent()) return exact.get().getId();

            // Tìm partial match
            String lower = suggestedCategory.toLowerCase();
            Optional<Category> partial = categories.stream()
                    .filter(c -> c.getName().toLowerCase().contains(lower)
                            || lower.contains(c.getName().toLowerCase()))
                    .findFirst();
            if (partial.isPresent()) return partial.get().getId();

        } catch (Exception e) {
            log.warn("[ReceiptScan] Match category failed: {}", e.getMessage());
        }

        return null;
    }
}
