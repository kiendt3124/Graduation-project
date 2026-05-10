package com.example.graduationproject.Service;

import com.example.graduationproject.Dto.Request.PartnerRequest;
import com.example.graduationproject.Dto.Response.PartnerResponse;
import com.example.graduationproject.Entity.Enum.PartnerType;
import com.example.graduationproject.Entity.Partner;
import com.example.graduationproject.Repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;

    // ─── ADMIN: Xem tất cả (cả inactive) ────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PartnerResponse> getAllPartnersForAdmin() {
        return partnerRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── END-USER: Xem danh sách đang hoạt động ─────────────────────────────

    @Transactional(readOnly = true)
    public List<PartnerResponse> getActivePartners() {
        return partnerRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── ADMIN: Tạo đối tác mới ──────────────────────────────────────────────

    @Transactional
    public PartnerResponse createPartner(PartnerRequest.Create req) {
        if (partnerRepository.existsByNameIgnoreCase(req.getName())) {
            throw new IllegalArgumentException("Đối tác '" + req.getName() + "' đã tồn tại.");
        }

        Partner partner = Partner.builder()
                .name(req.getName())
                .logoUrl(req.getLogoUrl())
                .partnerType(req.getPartnerType())
                .loanInterestRate(req.getLoanInterestRate())
                .savingInterestRate(req.getSavingInterestRate())
                .description(req.getDescription())
                .isActive(Boolean.TRUE)
                .build();

        partnerRepository.save(partner);
        log.info("Tạo đối tác mới: {} ({})", partner.getName(), partner.getPartnerType());
        return toResponse(partner);
    }

    // ─── ADMIN: Cập nhật thông tin / lãi suất ────────────────────────────────

    @Transactional
    public PartnerResponse updatePartner(PartnerRequest.Update req) {
        Partner partner = findById(req.getId());

        // Kiểm tra trùng tên nếu đổi tên
        if (req.getName() != null && !req.getName().equalsIgnoreCase(partner.getName())) {
            if (partnerRepository.existsByNameIgnoreCase(req.getName())) {
                throw new IllegalArgumentException("Tên đối tác '" + req.getName() + "' đã tồn tại.");
            }
            partner.setName(req.getName());
        }

        if (req.getLogoUrl() != null)          partner.setLogoUrl(req.getLogoUrl());
        if (req.getPartnerType() != null)       partner.setPartnerType(req.getPartnerType());
        if (req.getLoanInterestRate() != null)  partner.setLoanInterestRate(req.getLoanInterestRate());
        if (req.getSavingInterestRate() != null) partner.setSavingInterestRate(req.getSavingInterestRate());
        if (req.getDescription() != null)       partner.setDescription(req.getDescription());

        partnerRepository.save(partner);
        log.info("Cập nhật đối tác: {}", partner.getName());
        return toResponse(partner);
    }

    // ─── ADMIN: Bật/tắt hiển thị (toggle) ───────────────────────────────────

    @Transactional
    public PartnerResponse toggleActive(UUID id) {
        Partner partner = findById(id);
        partner.setIsActive(!partner.getIsActive());
        partnerRepository.save(partner);
        log.info("Toggle đối tác '{}': isActive = {}", partner.getName(), partner.getIsActive());
        return toResponse(partner);
    }

    // ─── ADMIN: Xóa vĩnh viễn ────────────────────────────────────────────────

    @Transactional
    public void deletePartner(UUID id) {
        Partner partner = findById(id);
        partnerRepository.delete(partner);
        log.info("Xóa đối tác: {}", partner.getName());
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    private Partner findById(UUID id) {
        return partnerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đối tác với ID: " + id));
    }

    private PartnerResponse toResponse(Partner p) {
        return PartnerResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .logoUrl(p.getLogoUrl())
                .partnerType(p.getPartnerType())
                .loanInterestRate(p.getLoanInterestRate())
                .savingInterestRate(p.getSavingInterestRate())
                .description(p.getDescription())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
