package group102.insurancefraud.service;

import group102.insurancefraud.dto.response.DashboardMetricsDto;
import group102.insurancefraud.entity.User;
import group102.insurancefraud.enums.ClaimStatus;
import group102.insurancefraud.repository.ClaimPredictionRepository;
import group102.insurancefraud.repository.ClaimShapFactorRepository;
import group102.insurancefraud.repository.RawClaimRepository;
import group102.insurancefraud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RawClaimRepository rawClaimRepository;
    private final UserRepository userRepository;
    private final ClaimPredictionRepository predictionRepository;
    private final ClaimShapFactorRepository shapFactorRepository;

    @Value("${app.dashboard.overdue-days:7}")
    private int overdueDays;

    public DashboardMetricsDto getMetricsForUser(User user, int daysRange) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(daysRange);

        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1).atStartOfDay();
        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);

        DashboardMetricsDto.DashboardMetricsDtoBuilder builder = DashboardMetricsDto.builder();

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            // ── Stat cards ────────────────────────────────────────
            long total = rawClaimRepository.count();
            long fraud = rawClaimRepository.countTotalRejectedClaims();
            builder.totalUsers(userRepository.count())
                   .totalClaims(total)
                   .totalFraudClaims(fraud)
                   .totalAmountPrevented(rawClaimRepository.sumTotalRejectedAmount())
                   .pendingClaims(rawClaimRepository.countPendingClaims())
                   .unassignedClaims(rawClaimRepository.countUnassignedClaims())
                   .suspiciousBeneficiaries(rawClaimRepository.countUniqueFraudulentBeneficiaries())
                   .suspiciousProviders(rawClaimRepository.countUniqueFraudulentProviders());

            // New Admin metrics
            builder.modelsRunToday(predictionRepository.countModelsRunByDateRange(startOfDay, now))
                   .modelsRunThisWeek(predictionRepository.countModelsRunByDateRange(startOfWeek, now))
                   .avgProcessingTimeDays(rawClaimRepository.getGlobalAvgProcessingTimeDays());

            long claimsThisMonth = rawClaimRepository.countClaimsByDateRange(startOfMonth, now);
            long fraudThisMonth = rawClaimRepository.countRejectedClaimsByDateRange(startOfMonth, now);
            long claimsLastMonth = rawClaimRepository.countClaimsByDateRange(startOfLastMonth, startOfMonth.minusSeconds(1));
            long fraudLastMonth = rawClaimRepository.countRejectedClaimsByDateRange(startOfLastMonth, startOfMonth.minusSeconds(1));
            
            builder.fraudRateThisMonth(claimsThisMonth > 0 ? (double) fraudThisMonth / claimsThisMonth : 0.0)
                   .fraudRateLastMonth(claimsLastMonth > 0 ? (double) fraudLastMonth / claimsLastMonth : 0.0)
                   .fraudTimeline(mapTimeline(rawClaimRepository.countFraudClaimsByMonth(startOfMonth.minusMonths(11)))) // Last 12 months
                   .topImportantFeatures(mapKeyValueDouble(shapFactorRepository.findTopFeaturesByImpact(PageRequest.of(0, 10))));

            // ── Charts ─────────────────────────────────────────────
            builder.statusDistribution(mapStatusDistribution(rawClaimRepository.countClaimsByStatus()));
            builder.claimsTimeline(mapTimeline(rawClaimRepository.countClaimsByDate(startDate)));

            // Top 5 fraud providers
            List<Object[]> providerRaw = rawClaimRepository.findTopFraudProvidersSummary(PageRequest.of(0, 5));
            builder.topFraudProviders(mapKeyValue(providerRaw));

            // Risk score buckets [0-25, 25-50, 50-75, 75-100]
            List<Object[]> buckets = predictionRepository.countRiskBuckets();
            builder.riskBuckets(flattenSingleRowAggregates(buckets));

        } else if ("INVESTIGATOR".equalsIgnoreCase(user.getRole())) {
            Long userId = user.getUserId();

            // ── Stat cards ────────────────────────────────────────
            long assigned = rawClaimRepository.countByInvestigator_UserId(userId);
            long fraudFound = rawClaimRepository.countByInvestigator_UserIdAndClaimStatus(userId, ClaimStatus.REJECTED);
            Double avgRisk = predictionRepository.avgRiskByInvestigator(userId);

            builder.claimsAssigned(assigned)
                   .claimsPendingReview(rawClaimRepository.countActiveClaimsByInvestigator(userId))
                   .fraudIdentified(fraudFound)
                   .alertsAssigned(predictionRepository.countAlertsByInvestigator(userId))
                   .avgRiskScore(avgRisk != null ? Math.round(avgRisk * 10.0) / 10.0 : 0.0);

            // New Investigator metrics
            builder.overdueCases(rawClaimRepository.countOverdueCasesByInvestigator(userId, now.minusDays(overdueDays)));
            
            long approved = rawClaimRepository.countByInvestigator_UserIdAndClaimStatus(userId, ClaimStatus.APPROVED);
            long closedCases = approved + fraudFound;
            builder.fraudConfirmationRate(closedCases > 0 ? (double) fraudFound / closedCases : 0.0);
            
            builder.topRiskUnprocessedClaims(mapKeyValueDouble(predictionRepository.findTopRiskUnprocessedClaims(userId, PageRequest.of(0, 5))));
            builder.topShapFeaturesByFreq(mapKeyValue(shapFactorRepository.findTopFeaturesByFrequencyForInvestigator(userId, PageRequest.of(0, 5))));
            builder.topShapFeaturesByImpact(mapKeyValueDouble(shapFactorRepository.findTopFeaturesByImpactForInvestigator(userId, PageRequest.of(0, 5))));

            // ── Charts ─────────────────────────────────────────────
            builder.statusDistribution(mapStatusDistribution(rawClaimRepository.countClaimsByStatusForInvestigator(userId)));
            builder.claimsTimeline(mapTimeline(rawClaimRepository.countClaimsByDateForInvestigator(startDate, userId)));

            // Risk groups [Thấp, Trung, Cao]
            List<Object[]> groups = predictionRepository.countRiskGroupsByInvestigator(userId);
            builder.riskGroups(flattenSingleRowAggregates(groups));

        } else if ("STAFF".equalsIgnoreCase(user.getRole())) {
            Long userId = user.getUserId();

            // ── Stat cards ────────────────────────────────────────
            long created = rawClaimRepository.countByClaimHandler_UserId(userId);
            long approved = rawClaimRepository.countByClaimHandler_UserIdAndClaimStatus(userId, ClaimStatus.APPROVED);
            long flagged = rawClaimRepository.countByClaimHandler_UserIdAndClaimStatus(userId, ClaimStatus.FLAGGED);
            long rejected = rawClaimRepository.countByClaimHandler_UserIdAndClaimStatus(userId, ClaimStatus.REJECTED);

            builder.claimsCreated(created)
                   .claimsApproved(approved)
                   .claimsFlagged(flagged)
                   .totalClaimAmount(rawClaimRepository.sumClmPmtAmtByStaff(userId))
                   .unassignedByStaff(rawClaimRepository.countUnassignedByStaff(userId));

            // New Staff Metrics
            builder.claimsCreatedToday(rawClaimRepository.countClaimsCreatedByDateRange(userId, startOfDay, now))
                   .claimsCreatedThisWeek(rawClaimRepository.countClaimsCreatedByDateRange(userId, startOfWeek, now))
                   .claimsRejected(rejected)
                   .claimsPending(rawClaimRepository.countByClaimHandler_UserIdAndClaimStatus(userId, ClaimStatus.PENDING))
                   .staffAvgProcessingTimeDays(rawClaimRepository.getStaffAvgProcessingTimeDays(userId));

            // ── Charts ─────────────────────────────────────────────
            builder.statusDistribution(mapStatusDistribution(rawClaimRepository.countClaimsByStatusForStaff(userId)));
            builder.claimsTimeline(mapTimeline(rawClaimRepository.countClaimsByDateForStaff(startDate, userId)));
        }

        return builder.build();
    }

    // ── Helpers ───────────────────────────────────────────────────
    private Map<String, Long> mapStatusDistribution(List<Object[]> rawList) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] obj : rawList) {
            if (obj[0] != null && obj[1] != null) {
                map.put(obj[0].toString(), ((Number) obj[1]).longValue());
            }
        }
        return map;
    }

    private List<DashboardMetricsDto.TimeSeriesData> mapTimeline(List<Object[]> rawList) {
        List<DashboardMetricsDto.TimeSeriesData> timeline = new ArrayList<>();
        for (Object[] obj : rawList) {
            if (obj[0] != null) {
                timeline.add(DashboardMetricsDto.TimeSeriesData.builder()
                        .date(obj[0].toString())
                        .count(((Number) obj[1]).longValue())
                        .build());
            }
        }
        return timeline;
    }

    private List<DashboardMetricsDto.KeyValueData> mapKeyValue(List<Object[]> rawList) {
        List<DashboardMetricsDto.KeyValueData> result = new ArrayList<>();
        for (Object[] obj : rawList) {
            if (obj[0] != null && obj[1] != null) {
                result.add(DashboardMetricsDto.KeyValueData.builder()
                        .key(obj[0].toString())
                        .value(((Number) obj[1]).longValue())
                        .build());
            }
        }
        return result;
    }

    private List<DashboardMetricsDto.KeyValueDataDouble> mapKeyValueDouble(List<Object[]> rawList) {
        List<DashboardMetricsDto.KeyValueDataDouble> result = new ArrayList<>();
        for (Object[] obj : rawList) {
            if (obj[0] != null && obj[1] != null) {
                result.add(DashboardMetricsDto.KeyValueDataDouble.builder()
                        .key(obj[0].toString())
                        .value(((Number) obj[1]).doubleValue())
                        .build());
            }
        }
        return result;
    }

    private List<Long> flattenSingleRowAggregates(List<Object[]> results) {
        List<Long> list = new ArrayList<>();
        if (results == null || results.isEmpty()) return list;
        Object first = results.get(0);
        if (first instanceof Object[]) {
            return objectArrayToLongList((Object[]) first);
        }
        for (Object o : results) {
            switch (o) {
                case null -> list.add(0L);
                case Number number -> list.add(number.longValue());
                case Object[] objects -> {
                    for (Object inner : objects) {
                        list.add(inner == null ? 0L : ((Number) inner).longValue());
                    }
                }
                default -> {
                }
            }
        }
        return list;
    }

    private List<Long> objectArrayToLongList(Object[] arr) {
        List<Long> list = new ArrayList<>();
        if (arr == null) return list;
        for (Object o : arr) {
            if (o == null) list.add(0L);
            else if (o instanceof Number) list.add(((Number) o).longValue());
            else list.add(0L);
        }
        return list;
    }
}
