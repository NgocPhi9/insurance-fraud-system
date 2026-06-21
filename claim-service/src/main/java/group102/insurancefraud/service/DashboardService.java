package group102.insurancefraud.service;

import group102.insurancefraud.dto.response.DashboardMetricsDto;
import group102.insurancefraud.entity.User;
import group102.insurancefraud.enums.ClaimStatus;
import group102.insurancefraud.repository.ClaimPredictionRepository;
import group102.insurancefraud.repository.ClaimShapFactorRepository;
import group102.insurancefraud.repository.RawClaimRepository;
import group102.insurancefraud.repository.UserRepository;
import group102.insurancefraud.util.FeatureLabelMap;
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
            long total = rawClaimRepository.countByCreatedAtGreaterThanEqual(startDate);
            long fraud = rawClaimRepository.countTotalRejectedClaimsSince(startDate);
            builder.totalUsers(userRepository.count())
                   .totalClaims(total)
                   .totalFraudClaims(fraud)
                   .totalAmountPrevented(rawClaimRepository.sumTotalRejectedAmountSince(startDate))
                   .pendingClaims(rawClaimRepository.countPendingClaimsSince(startDate))
                   .unassignedClaims(rawClaimRepository.countUnassignedClaimsSince(startDate))
                   .suspiciousBeneficiaries(rawClaimRepository.countUniqueFraudulentBeneficiariesSince(startDate))
                   .suspiciousProviders(rawClaimRepository.countUniqueFraudulentProvidersSince(startDate));

            // New Admin metrics
            builder.modelsRunToday(predictionRepository.countModelsRunByDateRange(startOfDay, now))
                   .modelsRunThisWeek(predictionRepository.countModelsRunByDateRange(startOfWeek, now))
                   .avgProcessingTimeDays(rawClaimRepository.getGlobalAvgProcessingTimeDaysSince(startDate));

            long claimsThisMonth = rawClaimRepository.countClaimsByDateRange(startOfMonth, now);
            long fraudThisMonth = rawClaimRepository.countRejectedClaimsByDateRange(startOfMonth, now);
            long claimsLastMonth = rawClaimRepository.countClaimsByDateRange(startOfLastMonth, startOfMonth.minusSeconds(1));
            long fraudLastMonth = rawClaimRepository.countRejectedClaimsByDateRange(startOfLastMonth, startOfMonth.minusSeconds(1));
            
            builder.fraudRateThisMonth(claimsThisMonth > 0 ? (double) fraudThisMonth / claimsThisMonth : 0.0)
                   .fraudRateLastMonth(claimsLastMonth > 0 ? (double) fraudLastMonth / claimsLastMonth : 0.0)
                   .fraudTimeline(mapTimeline(rawClaimRepository.countFraudClaimsByMonth(startDate))) // Filter timeline by startDate
                   .topImportantFeatures(mapKeyValueDoubleFeatureLabel(shapFactorRepository.findTopFeaturesByImpactSince(startDate, PageRequest.of(0, 10))));

            // ── Charts ─────────────────────────────────────────────
            builder.statusDistribution(mapStatusDistribution(rawClaimRepository.countClaimsByStatusSince(startDate)));
            builder.claimsTimeline(mapTimeline(rawClaimRepository.countClaimsByDate(startDate)));

            // Top 5 fraud providers
            List<Object[]> providerRaw = rawClaimRepository.findTopFraudProvidersSummarySince(startDate, PageRequest.of(0, 5));
            builder.topFraudProviders(mapKeyValue(providerRaw));

            // Risk score buckets [0-25, 25-50, 50-75, 75-100]
            List<Object[]> buckets = predictionRepository.countRiskBucketsSince(startDate);
            builder.riskBuckets(flattenSingleRowAggregates(buckets));

        } else if ("INVESTIGATOR".equalsIgnoreCase(user.getRole())) {
            Long userId = user.getUserId();

            // ── Stat cards ────────────────────────────────────────
            long assigned = rawClaimRepository.countByInvestigator_UserIdAndCreatedAtGreaterThanEqual(userId, startDate);
            long fraudFound = rawClaimRepository.countByInvestigator_UserIdAndClaimStatusAndCreatedAtGreaterThanEqual(userId, ClaimStatus.REJECTED, startDate);
            Double avgRisk = predictionRepository.avgRiskByInvestigatorSince(userId, startDate);

            builder.claimsAssigned(assigned)
                   .claimsPendingReview(rawClaimRepository.countActiveClaimsByInvestigatorSince(userId, startDate))
                   .fraudIdentified(fraudFound)
                   .alertsAssigned(predictionRepository.countAlertsByInvestigatorSince(userId, startDate))
                   .avgRiskScore(avgRisk != null ? Math.round(avgRisk * 10.0) / 10.0 : 0.0);

            // New Investigator metrics
            builder.overdueCases(rawClaimRepository.countOverdueCasesByInvestigator(userId, now.minusDays(overdueDays)));
            
            long approved = rawClaimRepository.countByInvestigator_UserIdAndClaimStatusAndCreatedAtGreaterThanEqual(userId, ClaimStatus.APPROVED, startDate);
            long closedCases = approved + fraudFound;
            builder.fraudConfirmationRate(closedCases > 0 ? (double) fraudFound / closedCases : 0.0);
            
            builder.topRiskUnprocessedClaims(mapKeyValueDouble(predictionRepository.findTopRiskUnprocessedClaimsSince(userId, startDate, PageRequest.of(0, 5))));
            builder.topShapFeaturesByFreq(mapKeyValueFeatureLabel(shapFactorRepository.findTopFeaturesByFrequencyForInvestigatorSince(userId, startDate, PageRequest.of(0, 5))));
            builder.topShapFeaturesByImpact(mapKeyValueDoubleFeatureLabel(shapFactorRepository.findTopFeaturesByImpactForInvestigatorSince(userId, startDate, PageRequest.of(0, 5))));

            // ── Charts ─────────────────────────────────────────────
            builder.statusDistribution(mapStatusDistribution(rawClaimRepository.countClaimsByStatusForInvestigatorSince(userId, startDate)));
            builder.claimsTimeline(mapTimeline(rawClaimRepository.countClaimsByDateForInvestigator(startDate, userId)));

            // Risk groups [Thấp, Trung, Cao]
            List<Object[]> groups = predictionRepository.countRiskGroupsByInvestigatorSince(userId, startDate);
            builder.riskGroups(flattenSingleRowAggregates(groups));

        } else if ("STAFF".equalsIgnoreCase(user.getRole())) {
            Long userId = user.getUserId();

            // ── Stat cards ────────────────────────────────────────
            long created = rawClaimRepository.countByClaimHandler_UserIdAndCreatedAtGreaterThanEqual(userId, startDate);
            long approved = rawClaimRepository.countByClaimHandler_UserIdAndClaimStatusAndCreatedAtGreaterThanEqual(userId, ClaimStatus.APPROVED, startDate);
            long flagged = rawClaimRepository.countByClaimHandler_UserIdAndClaimStatusAndCreatedAtGreaterThanEqual(userId, ClaimStatus.FLAGGED, startDate);
            long rejected = rawClaimRepository.countByClaimHandler_UserIdAndClaimStatusAndCreatedAtGreaterThanEqual(userId, ClaimStatus.REJECTED, startDate);

            builder.claimsCreated(created)
                   .claimsApproved(approved)
                   .claimsFlagged(flagged)
                   .totalClaimAmount(rawClaimRepository.sumClmPmtAmtByStaffSince(userId, startDate))
                   .unassignedByStaff(rawClaimRepository.countUnassignedByStaffSince(userId, startDate));

            // New Staff Metrics
            builder.claimsCreatedToday(rawClaimRepository.countClaimsCreatedByDateRange(userId, startOfDay, now))
                   .claimsCreatedThisWeek(rawClaimRepository.countClaimsCreatedByDateRange(userId, startOfWeek, now))
                   .claimsRejected(rejected)
                   .claimsPending(rawClaimRepository.countByClaimHandler_UserIdAndClaimStatusAndCreatedAtGreaterThanEqual(userId, ClaimStatus.PENDING, startDate))
                   .staffAvgProcessingTimeDays(rawClaimRepository.getStaffAvgProcessingTimeDaysSince(userId, startDate));

            // ── Charts ─────────────────────────────────────────────
            builder.statusDistribution(mapStatusDistribution(rawClaimRepository.countClaimsByStatusForStaffSince(userId, startDate)));
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

    private List<DashboardMetricsDto.KeyValueData> mapKeyValueFeatureLabel(List<Object[]> rawList) {
        List<DashboardMetricsDto.KeyValueData> result = new ArrayList<>();
        for (Object[] obj : rawList) {
            if (obj[0] != null && obj[1] != null) {
                result.add(DashboardMetricsDto.KeyValueData.builder()
                        .key(FeatureLabelMap.getLabel(obj[0].toString()))
                        .value(((Number) obj[1]).longValue())
                        .build());
            }
        }
        return result;
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

    private List<DashboardMetricsDto.KeyValueDataDouble> mapKeyValueDoubleFeatureLabel(List<Object[]> rawList) {
        List<DashboardMetricsDto.KeyValueDataDouble> result = new ArrayList<>();
        for (Object[] obj : rawList) {
            if (obj[0] != null && obj[1] != null) {
                result.add(DashboardMetricsDto.KeyValueDataDouble.builder()
                        .key(FeatureLabelMap.getLabel(obj[0].toString()))
                        .value(((Number) obj[1]).doubleValue())
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
