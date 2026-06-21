package group102.insurancefraud.service;

import group102.insurancefraud.dto.response.ReportRowDto;
import group102.insurancefraud.entity.ClaimPrediction;
import group102.insurancefraud.entity.RawClaim;
import group102.insurancefraud.entity.User;
import group102.insurancefraud.repository.ClaimPredictionRepository;
import group102.insurancefraud.repository.RawClaimRepository;
import group102.insurancefraud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final RawClaimRepository rawClaimRepository;
    private final ClaimPredictionRepository predictionRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ─── Data fetching ─────────────────────────────────────────────────────────

    public List<ReportRowDto.ClaimRow> getClaimsReport(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to   = toDate.atTime(23, 59, 59);

        List<RawClaim> claims = rawClaimRepository.findClaimsForReport(from, to);

        // Build risk score map: claimId -> latestRiskPercentage
        Map<Long, Double> riskMap = buildRiskMap(claims.stream().map(RawClaim::getRawClaimId).toList());

        return claims.stream().map(c -> ReportRowDto.ClaimRow.builder()
                .claimId(c.getRawClaimId())
                .desynpufId(c.getDesynpufId())
                .prvdrNum(c.getPrvdrNum())
                .claimStatus(c.getClaimStatus() != null ? c.getClaimStatus().name() : "")
                .clmPmtAmt(c.getClmPmtAmt())
                .clmFromDt(c.getClmFromDt())
                .clmThruDt(c.getClmThruDt())
                .claimHandlerName(c.getClaimHandler() != null ? c.getClaimHandler().getFullName() : "Chưa giao")
                .investigatorName(c.getInvestigator() != null ? c.getInvestigator().getFullName() : "Chưa giao")
                .createdAt(c.getCreatedAt())
                .riskPercentage(riskMap.get(c.getRawClaimId()))
                .build()
        ).toList();
    }

    public List<ReportRowDto.FraudRow> getFraudReport(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to   = toDate.atTime(23, 59, 59);

        List<RawClaim> claims = rawClaimRepository.findFraudClaimsForReport(from, to);
        Map<Long, Double> riskMap = buildRiskMap(claims.stream().map(RawClaim::getRawClaimId).toList());
        Map<Long, String> labelMap = buildLabelMap(claims.stream().map(RawClaim::getRawClaimId).toList());

        return claims.stream().map(c -> ReportRowDto.FraudRow.builder()
                .claimId(c.getRawClaimId())
                .desynpufId(c.getDesynpufId())
                .prvdrNum(c.getPrvdrNum())
                .clmPmtAmt(c.getClmPmtAmt())
                .clmFromDt(c.getClmFromDt())
                .clmThruDt(c.getClmThruDt())
                .investigatorName(c.getInvestigator() != null ? c.getInvestigator().getFullName() : "N/A")
                .resolvedAt(c.getResolvedAt())
                .riskPercentage(riskMap.get(c.getRawClaimId()))
                .predictedLabel(labelMap.get(c.getRawClaimId()))
                .build()
        ).toList();
    }

    /**
     * Hiệu suất nhân viên: lấy TẤT CẢ users có role STAFF hoặc INVESTIGATOR,
     * sau đó tổng hợp claims trong khoảng ngày cho từng người.
     * - STAFF: đếm theo claimHandler
     * - INVESTIGATOR: đếm theo investigator
     * Nhân viên không có claim trong range vẫn xuất hiện với count = 0.
     */
    public List<ReportRowDto.StaffPerformanceRow> getStaffPerformanceReport(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to   = toDate.atTime(23, 59, 59);

        // Lấy tất cả claims trong range (chỉ query 1 lần)
        List<RawClaim> allClaimsInRange = rawClaimRepository.findClaimsForReport(from, to);

        // Group theo claimHandler (STAFF)
        Map<Long, List<RawClaim>> byHandler = allClaimsInRange.stream()
                .filter(c -> c.getClaimHandler() != null)
                .collect(Collectors.groupingBy(c -> c.getClaimHandler().getUserId()));

        // Group theo investigator (INVESTIGATOR)
        Map<Long, List<RawClaim>> byInvestigator = allClaimsInRange.stream()
                .filter(c -> c.getInvestigator() != null)
                .collect(Collectors.groupingBy(c -> c.getInvestigator().getUserId()));

        // Lấy tất cả STAFF và INVESTIGATOR
        List<User> staffList        = userRepository.findByRoleIgnoreCase("STAFF");
        List<User> investigatorList = userRepository.findByRoleIgnoreCase("INVESTIGATOR");

        List<ReportRowDto.StaffPerformanceRow> rows = new ArrayList<>();

        // Xử lý từng STAFF
        for (User user : staffList) {
            List<RawClaim> claims = byHandler.getOrDefault(user.getUserId(), List.of());
            rows.add(buildPerformanceRow(user, claims));
        }

        // Xử lý từng INVESTIGATOR
        for (User user : investigatorList) {
            List<RawClaim> claims = byInvestigator.getOrDefault(user.getUserId(), List.of());
            rows.add(buildPerformanceRow(user, claims));
        }

        // Sắp xếp: nhiều claims nhất lên đầu
        rows.sort((a, b) -> Long.compare(b.getTotalClaims(), a.getTotalClaims()));
        return rows;
    }

    /** Helper: tính toán số liệu từ danh sách claims cho 1 nhân viên */
    private ReportRowDto.StaffPerformanceRow buildPerformanceRow(User user, List<RawClaim> claims) {
        long total    = claims.size();
        long approved = claims.stream().filter(c -> c.getClaimStatus() != null
                && "APPROVED".equals(c.getClaimStatus().name())).count();
        long rejected = claims.stream().filter(c -> c.getClaimStatus() != null
                && "REJECTED".equals(c.getClaimStatus().name())).count();
        BigDecimal amount = claims.stream()
                .filter(c -> c.getClmPmtAmt() != null)
                .map(RawClaim::getClmPmtAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double fraudRate = total > 0 ? (double) rejected / total * 100.0 : 0.0;

        return ReportRowDto.StaffPerformanceRow.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .role(user.getRole())
                .totalClaims(total)
                .approvedClaims(approved)
                .rejectedClaims(rejected)
                .pendingClaims(total - approved - rejected)
                .totalAmount(amount)
                .fraudRate(Math.round(fraudRate * 10.0) / 10.0)
                .build();
    }

    // ─── Excel export ──────────────────────────────────────────────────────────

    public byte[] exportToExcel(String reportType, LocalDate fromDate, LocalDate toDate) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle   = createDataStyle(workbook);
            CellStyle moneyStyle  = createMoneyStyle(workbook);
            CellStyle riskHighStyle  = createRiskStyle(workbook, IndexedColors.ROSE);
            CellStyle riskMedStyle   = createRiskStyle(workbook, IndexedColors.LIGHT_YELLOW);

            switch (reportType.toUpperCase()) {
                case "CLAIMS" -> writeClaimsSheet(workbook, headerStyle, dataStyle, moneyStyle,
                        riskHighStyle, riskMedStyle, fromDate, toDate);
                case "FRAUD"  -> writeFraudSheet(workbook, headerStyle, dataStyle, moneyStyle,
                        riskHighStyle, fromDate, toDate);
                case "STAFF_PERFORMANCE" -> writeStaffSheet(workbook, headerStyle, dataStyle,
                        moneyStyle, fromDate, toDate);
                default -> {
                    // Xuất tất cả 3 sheets
                    writeClaimsSheet(workbook, headerStyle, dataStyle, moneyStyle,
                            riskHighStyle, riskMedStyle, fromDate, toDate);
                    writeFraudSheet(workbook, headerStyle, dataStyle, moneyStyle,
                            riskHighStyle, fromDate, toDate);
                    writeStaffSheet(workbook, headerStyle, dataStyle, moneyStyle, fromDate, toDate);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ─── Sheet writers ─────────────────────────────────────────────────────────

    private void writeClaimsSheet(XSSFWorkbook wb, CellStyle hStyle, CellStyle dStyle,
                                   CellStyle mStyle, CellStyle rHighStyle, CellStyle rMedStyle,
                                   LocalDate from, LocalDate to) {
        Sheet sheet = wb.createSheet("Tổng quan Claims");
        String[] headers = {
            "ID", "Beneficiary ID", "Provider", "Trạng thái",
            "Số tiền ($)", "Từ ngày", "Đến ngày",
            "Staff phụ trách", "Điều tra viên", "Ngày tạo", "Risk Score (%)"
        };
        writeHeader(sheet, hStyle, headers, from, to);
        autoSizeColumns(sheet, headers.length);

        List<ReportRowDto.ClaimRow> rows = getClaimsReport(from, to);
        int rowNum = 3;
        for (ReportRowDto.ClaimRow r : rows) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, r.getClaimId()     != null ? r.getClaimId().toString() : "",   dStyle);
            createCell(row, 1, r.getDesynpufId()  != null ? r.getDesynpufId()   : "",          dStyle);
            createCell(row, 2, r.getPrvdrNum()    != null ? r.getPrvdrNum()     : "",          dStyle);
            createCell(row, 3, r.getClaimStatus() != null ? r.getClaimStatus()  : "",          dStyle);
            createMoneyCell(row, 4, r.getClmPmtAmt(), mStyle);
            createCell(row, 5, r.getClmFromDt() != null ? r.getClmFromDt().format(DATE_FMT) : "", dStyle);
            createCell(row, 6, r.getClmThruDt() != null ? r.getClmThruDt().format(DATE_FMT) : "", dStyle);
            createCell(row, 7, r.getClaimHandlerName(), dStyle);
            createCell(row, 8, r.getInvestigatorName(), dStyle);
            createCell(row, 9, r.getCreatedAt() != null ? r.getCreatedAt().format(DATETIME_FMT) : "", dStyle);

            // Risk score với màu sắc
            Double risk = r.getRiskPercentage();
            CellStyle rStyle = (risk != null && risk >= 70) ? rHighStyle
                             : (risk != null && risk >= 40) ? rMedStyle : dStyle;
            createCell(row, 10, risk != null ? String.format("%.1f%%", risk) : "N/A", rStyle);
        }
    }

    private void writeFraudSheet(XSSFWorkbook wb, CellStyle hStyle, CellStyle dStyle,
                                  CellStyle mStyle, CellStyle rHighStyle,
                                  LocalDate from, LocalDate to) {
        Sheet sheet = wb.createSheet("Báo cáo Gian lận");
        String[] headers = {
            "ID", "Beneficiary ID", "Provider", "Số tiền ($)",
            "Từ ngày", "Đến ngày", "Điều tra viên", "Ngày xử lý", "Risk Score (%)", "Kết quả ML"
        };
        writeHeader(sheet, hStyle, headers, from, to);
        autoSizeColumns(sheet, headers.length);

        List<ReportRowDto.FraudRow> rows = getFraudReport(from, to);
        int rowNum = 3;
        for (ReportRowDto.FraudRow r : rows) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, r.getClaimId()  != null ? r.getClaimId().toString() : "", dStyle);
            createCell(row, 1, r.getDesynpufId()  != null ? r.getDesynpufId()  : "", dStyle);
            createCell(row, 2, r.getPrvdrNum()    != null ? r.getPrvdrNum()    : "", dStyle);
            createMoneyCell(row, 3, r.getClmPmtAmt(), mStyle);
            createCell(row, 4, r.getClmFromDt() != null ? r.getClmFromDt().format(DATE_FMT) : "", dStyle);
            createCell(row, 5, r.getClmThruDt() != null ? r.getClmThruDt().format(DATE_FMT) : "", dStyle);
            createCell(row, 6, r.getInvestigatorName() != null ? r.getInvestigatorName() : "", dStyle);
            createCell(row, 7, r.getResolvedAt() != null ? r.getResolvedAt().format(DATETIME_FMT) : "", dStyle);
            Double risk = r.getRiskPercentage();
            CellStyle rStyle = (risk != null && risk >= 70) ? rHighStyle : dStyle;
            createCell(row, 8, risk != null ? String.format("%.1f%%", risk) : "N/A", rStyle);
            createCell(row, 9, r.getPredictedLabel() != null ? r.getPredictedLabel() : "", dStyle);
        }
    }

    private void writeStaffSheet(XSSFWorkbook wb, CellStyle hStyle, CellStyle dStyle,
                                  CellStyle mStyle, LocalDate from, LocalDate to) {
        Sheet sheet = wb.createSheet("Hiệu suất Nhân viên");
        String[] headers = {
            "ID NV", "Họ và tên", "Vai trò", "Tổng claims",
            "Đã duyệt", "Từ chối (Gian lận)", "Đang xử lý",
            "Tổng tiền ($)", "Tỷ lệ Gian lận (%)"
        };
        writeHeader(sheet, hStyle, headers, from, to);
        autoSizeColumns(sheet, headers.length);

        List<ReportRowDto.StaffPerformanceRow> rows = getStaffPerformanceReport(from, to);
        int rowNum = 3;
        for (ReportRowDto.StaffPerformanceRow r : rows) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, r.getUserId() != null ? r.getUserId().toString() : "", dStyle);
            createCell(row, 1, r.getFullName(), dStyle);
            createCell(row, 2, r.getRole(), dStyle);
            createCell(row, 3, String.valueOf(r.getTotalClaims()), dStyle);
            createCell(row, 4, String.valueOf(r.getApprovedClaims()), dStyle);
            createCell(row, 5, String.valueOf(r.getRejectedClaims()), dStyle);
            createCell(row, 6, String.valueOf(r.getPendingClaims()), dStyle);
            createMoneyCell(row, 7, r.getTotalAmount(), mStyle);
            createCell(row, 8, String.format("%.1f%%", r.getFraudRate()), dStyle);
        }
    }

    // ─── Style helpers ─────────────────────────────────────────────────────────

    private void writeHeader(Sheet sheet, CellStyle hStyle, String[] headers,
                              LocalDate from, LocalDate to) {
        // Row 0: title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Báo cáo từ " + from.format(DATE_FMT) + " đến " + to.format(DATE_FMT));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));

        // Row 1: blank
        sheet.createRow(1);

        // Row 2: column headers
        Row headerRow = sheet.createRow(2);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(hStyle);
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        Font font = wb.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(false);
        return style;
    }

    private CellStyle createMoneyStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createRiskStyle(Workbook wb, IndexedColors color) {
        CellStyle style = createDataStyle(wb);
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createMoneyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.setScale(2, RoundingMode.HALF_UP).doubleValue());
        } else {
            cell.setCellValue(0.0);
        }
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.setColumnWidth(i, 5000); // ~180px default
        }
    }

    // ─── Prediction helpers ────────────────────────────────────────────────────

    private Map<Long, Double> buildRiskMap(List<Long> claimIds) {
        return claimIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            Optional<ClaimPrediction> pred =
                                    predictionRepository.findTopByRawClaim_RawClaimIdOrderByPredictedAtDesc(id);
                            return pred.map(ClaimPrediction::getRiskPercentage).orElse(null);
                        }
                ));
    }

    private Map<Long, String> buildLabelMap(List<Long> claimIds) {
        return claimIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            Optional<ClaimPrediction> pred =
                                    predictionRepository.findTopByRawClaim_RawClaimIdOrderByPredictedAtDesc(id);
                            return pred.map(ClaimPrediction::getPredictedLabel).orElse("N/A");
                        }
                ));
    }
}
