package group102.insurancefraud.service;

import group102.insurancefraud.config.AppConfig;
import group102.insurancefraud.dto.response.FraudSuspectDto;
import group102.insurancefraud.repository.RawClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FraudIntelligenceService {

    private final RawClaimRepository rawClaimRepository;
    private final AppConfig appConfig;

    public List<FraudSuspectDto> getTopFraudulentBeneficiaries() {
        Pageable pageable = PageRequest.of(0, appConfig.getPageSize());
        List<Object[]> results = rawClaimRepository.findTopFraudulentBeneficiaries(appConfig.getMinFraudClaims(), pageable);
        return results.stream().map(row -> {
            String entityId = (String) row[0];
            long fraudCount = ((Number) row[1]).longValue();
            BigDecimal amount = (BigDecimal) row[2];
            return FraudSuspectDto.builder()
                    .entityId(entityId)
                    .fraudCount(fraudCount)
                    .totalAmountAtRisk(amount)
                    .riskLevel(determineRiskLevel(fraudCount))
                    .build();
        }).collect(Collectors.toList());
    }

    public List<FraudSuspectDto> getTopFraudulentProviders() {
        Pageable pageable = PageRequest.of(0, appConfig.getPageSize());
        List<Object[]> results = rawClaimRepository.findTopFraudulentProviders(appConfig.getMinFraudClaims(), pageable);
        return results.stream().map(row -> {
            String entityId = (String) row[0];
            long fraudCount = ((Number) row[1]).longValue();
            BigDecimal amount = (BigDecimal) row[2];
            return FraudSuspectDto.builder()
                    .entityId(entityId)
                    .fraudCount(fraudCount)
                    .totalAmountAtRisk(amount)
                    .riskLevel(determineRiskLevel(fraudCount))
                    .build();
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getSummaryStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBeneficiaries", rawClaimRepository.countUniqueFraudulentBeneficiaries());
        stats.put("totalProviders", rawClaimRepository.countUniqueFraudulentProviders());
        stats.put("totalRejectedClaims", rawClaimRepository.countTotalRejectedClaims());
        stats.put("totalRejectedAmount", rawClaimRepository.sumTotalRejectedAmount());
        return stats;
    }

    private String determineRiskLevel(long fraudCount) {
        if (fraudCount >= 5) {
            return "RẤT CAO";
        } else if (fraudCount >= 3) {
            return "CAO";
        } else if (fraudCount >= 2) {
            return "TRUNG BÌNH";
        } else {
            return "THẤP";
        }
    }
}
