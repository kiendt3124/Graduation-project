package com.example.graduationproject.Repository;

import com.example.graduationproject.Entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    /**
     * Lấy danh sách đối tác đang hiển thị (isActive = true),
     * dùng cho end-user xem.
     */
    List<Partner> findByIsActiveTrueOrderByNameAsc();

    /**
     * Kiểm tra tên đối tác đã tồn tại chưa (tránh trùng lặp).
     */
    boolean existsByNameIgnoreCase(String name);
}
