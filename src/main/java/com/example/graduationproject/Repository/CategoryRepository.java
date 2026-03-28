package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}
