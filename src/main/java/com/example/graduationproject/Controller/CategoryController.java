package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Request.CreateCategoryRequest;
import com.example.graduationproject.Dto.Request.UpdateCategoryRequest;
import com.example.graduationproject.Dto.Response.CategoryResponse;
import com.example.graduationproject.Service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "Quản lý danh mục thu/chi (Ăn uống, Lương, Di chuyển...)")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
        summary = "Tạo danh mục",
        description = "Tạo danh mục mới. `transactionType` nhận: `INCOME` hoặc `EXPENSE`.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tạo thành công",
                content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Lỗi dữ liệu",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody CreateCategoryRequest req) {
        try {
            return ResponseEntity.ok(categoryService.createCategory(req));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @Operation(
        summary = "Lấy tất cả danh mục",
        description = "Trả về toàn bộ danh mục. Frontend có thể filter theo `transactionType` ở phía client.",
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

    @Operation(
        summary = "Cập nhật danh mục",
        description = "Cập nhật tên hoặc icon. `id` bắt buộc, các field còn lại tuỳ chọn.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy danh mục",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @PatchMapping
    public ResponseEntity<?> updateCategory(@Valid @RequestBody UpdateCategoryRequest req) {
        try {
            return ResponseEntity.ok(categoryService.updateCategory(req));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @Operation(
        summary = "Xóa danh mục",
        description = "⚠️ Xóa vĩnh viễn danh mục và toàn bộ giao dịch liên quan (do orphanRemoval = true).",
        responses = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "400", description = "Không tìm thấy danh mục",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(
            @Parameter(description = "ID của danh mục cần xóa", required = true) @PathVariable UUID id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok("Category deleted successfully");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
