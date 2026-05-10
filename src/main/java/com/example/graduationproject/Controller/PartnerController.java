package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Response.PartnerResponse;
import com.example.graduationproject.Service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
@Tag(name = "Partner", description = "Danh sách đối tác vay vốn & đầu tư — dành cho end-user xem tham khảo lãi suất")
@SecurityRequirement(name = "bearerAuth")
public class PartnerController {

    private final PartnerService partnerService;

    @Operation(
        summary = "Danh sách đối tác đang hoạt động",
        description = "Trả về danh sách các ngân hàng, quỹ đầu tư đang được hiển thị " +
                      "(isActive = true). Người dùng có thể xem lãi suất vay/tiết kiệm để tham khảo.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = PartnerResponse.class))))
        }
    )
    @GetMapping
    public ResponseEntity<List<PartnerResponse>> getActivePartners() {
        return ResponseEntity.ok(partnerService.getActivePartners());
    }
}
