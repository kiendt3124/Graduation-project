package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Response.CategoryResponse;
import com.example.graduationproject.Service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "Danh mục thu/chi hệ thống — chỉ đọc dành cho end-user. Tạo/Sửa/Xóa danh mục tại /api/admin/categories")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
        summary = "Lấy tất cả danh mục",
        description = "Trả về toàn bộ danh mục hệ thống. " +
                      "Có thể filter theo `transactionType` (`INCOME` | `EXPENSE`) ở phía client.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách danh mục",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class))))
        }
    )
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @Operation(
        summary = "Lấy danh mục theo ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tìm thấy",
                content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(
            @Parameter(description = "ID của danh mục", required = true) @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(categoryService.getCategoryById(id));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
