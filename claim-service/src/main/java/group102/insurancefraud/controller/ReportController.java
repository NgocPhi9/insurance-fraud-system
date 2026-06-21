package group102.insurancefraud.controller;

import group102.insurancefraud.dto.response.ReportRowDto;
import group102.insurancefraud.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ReportController extends BaseController {

    private final ReportService reportService;

    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ─── Trang báo cáo ────────────────────────────────────────────────────────

    @GetMapping
    public String reportPage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "CLAIMS") String tab,
            Model model) {

        // Mặc định: 30 ngày gần nhất
        LocalDate toDate   = to   != null ? to   : LocalDate.now();
        LocalDate fromDate = from != null ? from : toDate.minusDays(30);

        List<ReportRowDto.ClaimRow>            claimsData = reportService.getClaimsReport(fromDate, toDate);
        List<ReportRowDto.FraudRow>             fraudData = reportService.getFraudReport(fromDate, toDate);
        List<ReportRowDto.StaffPerformanceRow> staffData  = reportService.getStaffPerformanceReport(fromDate, toDate);

        // Summary numbers
        long totalClaims  = claimsData.size();
        long totalFraud   = fraudData.size();
        double fraudRate  = totalClaims > 0 ? (double) totalFraud / totalClaims * 100.0 : 0.0;
        double totalPrevented = fraudData.stream()
                .mapToDouble(r -> r.getClmPmtAmt() != null ? r.getClmPmtAmt().doubleValue() : 0.0)
                .sum();

        model.addAttribute("activePage", "reports");
        model.addAttribute("breadcrumbCurrent", "Báo cáo");
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("activeTab", tab);

        model.addAttribute("claimsData", claimsData);
        model.addAttribute("fraudData", fraudData);
        model.addAttribute("staffData", staffData);

        model.addAttribute("totalClaims", totalClaims);
        model.addAttribute("totalFraud", totalFraud);
        model.addAttribute("fraudRate", Math.round(fraudRate * 10.0) / 10.0);
        model.addAttribute("totalPrevented", totalPrevented);

        return "reports/index";
    }

    // ─── Xuất Excel ───────────────────────────────────────────────────────────

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        try {
            byte[] data = reportService.exportToExcel(type, from, to);

            String filename = String.format("BaoCao_%s_%s_%s.xlsx",
                    type, from.format(FILE_DATE_FMT), to.format(FILE_DATE_FMT));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);

        } catch (IOException e) {
            log.error("Lỗi xuất Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
