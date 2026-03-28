package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Request.CreateCategoryRequest;
import com.example.graduationproject.Dto.Request.UpdateCategoryRequest;
import com.example.graduationproject.Dto.Response.CategoryResponse;
import com.example.graduationproject.Entity.Category;
import com.example.graduationproject.Repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    public CategoryResponse createCategory(CreateCategoryRequest req) {
        Category category = Category.builder()
                .name(req.getName())
                .icon(req.getIcon())
                .transactionType(req.getTransactionType())
                .build();
        categoryRepository.save(category);
        return toResponse(category);
    }

    // ─── GET ALL ─────────────────────────────────────────────────────────────

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── GET BY ID ───────────────────────────────────────────────────────────

    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return toResponse(category);
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    public CategoryResponse updateCategory(UpdateCategoryRequest req) {
        Category category = categoryRepository.findById(req.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (req.getName() != null) {
            category.setName(req.getName());
        }
        if (req.getIcon() != null) {
            category.setIcon(req.getIcon());
        }

        categoryRepository.save(category);
        return toResponse(category);
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        categoryRepository.delete(category);
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .icon(c.getIcon())
                .transactionType(c.getTransactionType())
                .build();
    }
}
