package group102.insurancefraud.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Container class for report row DTOs.
 * Each inner class represents one row in a specific report type.
 */
public class ReportRowDto {

    /** Dòng dữ liệu cho báo cáo Tổng quan Claims */
    @Data
    @Builder
    public static class ClaimRow {
        private Long claimId;
        private String desynpufId;       // Beneficiary ID
        private String prvdrNum;         // Provider Number
        private String claimStatus;
        private BigDecimal clmPmtAmt;
        private LocalDate clmFromDt;
        private LocalDate clmThruDt;
        private String claimHandlerName; // Staff phụ trách
        private String investigatorName; // Điều tra viên
        private LocalDateTime createdAt;
        private Double riskPercentage;   // ML risk score (nullable)
    }

    /** Dòng dữ liệu cho báo cáo Gian lận */
    @Data
    @Builder
    public static class FraudRow {
        private Long claimId;
        private String desynpufId;
        private String prvdrNum;
        private BigDecimal clmPmtAmt;
        private LocalDate clmFromDt;
        private LocalDate clmThruDt;
        private String investigatorName;
        private LocalDateTime resolvedAt;
        private Double riskPercentage;
        private String predictedLabel;
    }

    /** Dòng dữ liệu cho báo cáo Hiệu suất Nhân viên */
    @Data
    @Builder
    public static class StaffPerformanceRow {
        private Long userId;
        private String fullName;
        private String role;
        private Long totalClaims;
        private Long approvedClaims;
        private Long rejectedClaims;
        private Long pendingClaims;
        private BigDecimal totalAmount;
        private Double fraudRate;        // rejectedClaims / totalClaims * 100
    }
}
