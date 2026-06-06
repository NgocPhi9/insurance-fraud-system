package group102.insurancefraud.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardMetricsDto {
    // Shared
    private Map<String, Long> statusDistribution;
    private List<TimeSeriesData> claimsTimeline;

    // ── ADMIN metrics ──────────────────────────────────────────
    private Long totalUsers;
    private Long totalClaims;
    private Long totalFraudClaims;
    private BigDecimal totalAmountPrevented;
    /** Số claims đang PENDING */
    private Long pendingClaims;
    /** Số claims PENDING chưa được giao điều tra viên */
    private Long unassignedClaims;
    /** Số ML alerts đang mở (shouldAlert=true) */
    private Long openAlerts;
    /** Số providers duy nhất có claim bị REJECTED */
    private Long suspiciousProviders;
    /** Top N providers gian lận: [prvdrNum, count] */
    private List<KeyValueData> topFraudProviders;
    /** Risk score phân bổ 4 nhóm: [0-25, 25-50, 50-75, 75-100] */
    private List<Long> riskBuckets;

    // ── STAFF metrics ──────────────────────────────────────────
    private Long claimsCreated;
    private Long claimsApproved;
    private Long claimsFlagged;
    /** Tổng số tiền claim của staff */
    private BigDecimal totalClaimAmount;
    /** Số claims của staff chưa có điều tra viên */
    private Long unassignedByStaff;

    // ── INVESTIGATOR metrics ───────────────────────────────────
    private Long claimsAssigned;
    private Long claimsPendingReview;
    private Long fraudIdentified;
    /** Số claims ML alert được giao cho investigator */
    private Long alertsAssigned;
    /** Trung bình risk score (%) của hồ sơ được giao */
    private Double avgRiskScore;
    /** Risk groups: [Thấp (<40%), Trung (40-70%), Cao (>70%)] */
    private List<Long> riskGroups;

    // ── Nested types ───────────────────────────────────────────
    @Data
    @Builder
    public static class TimeSeriesData {
        private String date;
        private Long count;
    }

    @Data
    @Builder
    public static class KeyValueData {
        private String key;
        private Long value;
    }
}
