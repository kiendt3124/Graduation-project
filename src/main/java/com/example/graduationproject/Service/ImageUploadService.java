package com.example.graduationproject.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Service upload ảnh lên Cloudinary.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    /**
     * Validate và upload ảnh lên Cloudinary.
     *
     * @param file ảnh từ client
     * @return secure URL của ảnh đã upload
     */
    @SuppressWarnings("unchecked")
    public String uploadImage(MultipartFile file) {
        validateImage(file);

        try {
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "receipts",
                            "resource_type", "image"
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            log.info("[Cloudinary] Upload OK → {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("[Cloudinary] Upload thất bại: {}", e.getMessage());
            throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage(), e);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File quá lớn. Tối đa 5MB, file hiện tại: "
                            + (file.getSize() / 1024 / 1024) + "MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Chỉ chấp nhận ảnh JPG, PNG, WebP. Loại file: " + contentType);
        }
    }
}
