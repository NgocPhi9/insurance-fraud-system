package group102.insurancefraud.service;

import group102.insurancefraud.dto.response.DashboardMetricsDto;
import group102.insurancefraud.entity.User;
import group102.insurancefraud.enums.ClaimStatus;
import group102.insurancefraud.repository.ClaimPredictionRepository;
import group102.insurancefraud.repository.RawClaimRepository;
import group102.insurancefraud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    public DashboardMetricsDto getMetricsForUser(User user, int daysRange) {
        LocalDate startDate = LocalDate.now().minusDays(daysRange);
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
                   .openAlerts(predictionRepository.countOpenAlerts())
                   .suspiciousProviders(rawClaimRepository.countUniqueFraudulentProviders());

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

            builder.claimsCreated(created)
                   .claimsApproved(approved)
                   .claimsFlagged(flagged)
                   .totalClaimAmount(rawClaimRepository.sumClmPmtAmtByStaff(userId))
                   .unassignedByStaff(rawClaimRepository.countUnassignedByStaff(userId));

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
                        .count((Long) obj[1])
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

    /**
     * For single-row aggregate queries that return List<Object[]> (one row, multiple columns).
     * E.g.: SELECT SUM(...), SUM(...), SUM(...) -> returns [[val1, val2, val3]]
     */
    private List<Long> flattenSingleRowAggregates(List<Object[]> results) {
        List<Long> list = new ArrayList<>();
        if (results == null || results.isEmpty()) return list;
        Object first = results.get(0);
        // Single-row multi-column: first element IS the Object[] of column values
        if (first instanceof Object[]) {
            return objectArrayToLongList((Object[]) first);
        }
        // Edge case: might already be unwrapped scalars in the list
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
