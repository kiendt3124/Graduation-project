package com.example.graduationproject.Controller;

import com.example.graduationproject.Dto.Response.*;
import com.example.graduationproject.Service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Report", description = "Báo cáo & Thống kê tài chính")
public class ReportController {

    private final ReportService reportService;

    // ─── 1. TỔNG QUAN ────────────────────────────────────────────────────────

    @Operation(
        summary = "Tổng quan tài chính tháng",
        description = """
            Trả về bức tranh toàn cảnh tài chính trong tháng/năm chỉ định:
            - Tổng thu, tổng chi, dòng tiền ròng
            - Tổng số dư tất cả ví
            - Số khoản vay đang active, tổng dư nợ đi vay và cho vay
            - Số mục tiêu tài chính đang thực hiện và đã hoàn thành
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Thành công",
                content = @Content(schema = @Schema(implementation = ReportSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Tham số không hợp lệ",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            @Parameter(description = "Tháng (1–12)", required = true, example = "4")
            @RequestParam int month,
            @Parameter(description = "Năm (ví dụ: 2026)", required = true, example = "2026")
            @RequestParam int year) {
        try {
            String email = getEmail();
            ReportSummaryResponse response = reportService.getSummary(email, month, year);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── 2. CHI TIÊU THEO DANH MỤC ──────────────────────────────────────────

    @Operation(
        summary = "Chi tiêu theo danh mục",
        description = """
            Trả về danh sách **EXPENSE** nhóm theo danh mục trong tháng/năm.
            Mỗi item gồm: tên danh mục, tổng chi, và tỉ lệ % so với tổng chi tháng.
            Sắp xếp theo tổng chi giảm dần (danh mục tốn nhiều nhất ở đầu).
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách chi tiêu theo danh mục",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryExpenseResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Lỗi",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping("/expense-by-category")
    public ResponseEntity<?> getExpenseByCategory(
            @Parameter(description = "Tháng (1–12)", required = true) @RequestParam int month,
            @Parameter(description = "Năm", required = true) @RequestParam int year) {
        try {
            String email = getEmail();
            List<CategoryExpenseResponse> response = reportService.getExpenseByCategory(email, month, year);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── 3. THU NHẬP THEO DANH MỤC ──────────────────────────────────────────

    @Operation(
        summary = "Thu nhập theo danh mục",
        description = """
            Trả về danh sách **INCOME** nhóm theo danh mục trong tháng/năm.
            Mỗi item gồm: tên danh mục, tổng thu, và tỉ lệ % so với tổng thu tháng.
            Sắp xếp theo tổng thu giảm dần.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách thu nhập theo danh mục",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryExpenseResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Lỗi",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping("/income-by-category")
    public ResponseEntity<?> getIncomeByCategory(
            @Parameter(description = "Tháng (1–12)", required = true) @RequestParam int month,
            @Parameter(description = "Năm", required = true) @RequestParam int year) {
        try {
            String email = getEmail();
            List<CategoryExpenseResponse> response = reportService.getIncomeByCategory(email, month, year);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── 4. DÒNG TIỀN THEO NGÀY ─────────────────────────────────────────────

    @Operation(
        summary = "Dòng tiền theo ngày",
        description = """
            Trả về dòng tiền thu/chi/ròng theo từng ngày trong khoảng [from, to].
            
            - Format ngày: `yyyy-MM-dd` (ví dụ: `2026-04-01`)
            - Những ngày không có giao dịch sẽ không xuất hiện trong response
            - Phù hợp để vẽ biểu đồ đường (line chart) trên mobile
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách dòng tiền theo ngày",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = CashFlowByDayResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Tham số không hợp lệ (from > to, null, ...)",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping("/cash-flow")
    public ResponseEntity<?> getCashFlowByDay(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)", required = true, example = "2026-04-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)", required = true, example = "2026-04-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            String email = getEmail();
            List<CashFlowByDayResponse> response = reportService.getCashFlowByDay(email, from, to);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── 5. BÁO CÁO 12 THÁNG ────────────────────────────────────────────────

    @Operation(
        summary = "Báo cáo 12 tháng trong năm",
        description = """
            Trả về tổng thu/chi/ròng cho cả 12 tháng trong năm chỉ định.
            Luôn trả đủ 12 phần tử, tháng không có giao dịch sẽ có income=0, expense=0.
            Phù hợp để vẽ biểu đồ cột (bar chart) so sánh thu chi theo tháng.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Báo cáo 12 tháng",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = MonthlyReportResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Lỗi",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyReport(
            @Parameter(description = "Năm cần xem báo cáo", required = true, example = "2026")
            @RequestParam int year) {
        try {
            String email = getEmail();
            List<MonthlyReportResponse> response = reportService.getMonthlyReport(email, year);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── 6. NGÂN SÁCH VS. THỰC TẾ ──────────────────────────────────────────

    @Operation(
        summary = "So sánh ngân sách với thực chi",
        description = """
            Trả về danh sách các danh mục đã đặt ngân sách trong tháng/năm,
            kèm số tiền thực tế đã chi, số tiền còn lại, tỉ lệ sử dụng, và cờ `isOverBudget`.
            
            Chỉ trả về các danh mục có budget được tạo cho tháng đó.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Danh sách so sánh ngân sách",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = BudgetVsActualResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Lỗi",
                content = @Content(schema = @Schema(type = "string")))
        }
    )
    @GetMapping("/budget-vs-actual")
    public ResponseEntity<?> getBudgetVsActual(
            @Parameter(description = "Tháng (1–12)", required = true) @RequestParam int month,
            @Parameter(description = "Năm", required = true) @RequestParam int year) {
        try {
            String email = getEmail();
            List<BudgetVsActualResponse> response = reportService.getBudgetVsActual(email, month, year);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ─── HELPER ─────────────────────────────────────────────────────────────

    private String getEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
