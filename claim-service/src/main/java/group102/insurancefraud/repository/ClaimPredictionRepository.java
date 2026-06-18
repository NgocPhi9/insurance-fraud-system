package group102.insurancefraud.repository;

import group102.insurancefraud.entity.ClaimPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimPredictionRepository extends JpaRepository<ClaimPrediction, Long> {

    // Tìm prediction mới nhất của 1 claim
    Optional<ClaimPrediction> findTopByRawClaim_RawClaimIdOrderByPredictedAtDesc(Long rawClaimId);

    // Tìm tất cả prediction của 1 claim
    List<ClaimPrediction> findByRawClaim_RawClaimId(Long rawClaimId);

    // Tìm các claim bị ANOMALY
    List<ClaimPrediction> findByPredictedLabel(String predictedLabel);

    // Tìm các claim cần alert
    List<ClaimPrediction> findByShouldAlertTrue();

    // ── ADMIN extras ─────────────────────────────────────────────
    @Query("SELECT COUNT(p) FROM ClaimPrediction p WHERE p.shouldAlert = true")
    long countOpenAlerts();

    @Query("SELECT p.predictedLabel, COUNT(p) FROM ClaimPrediction p GROUP BY p.predictedLabel")
    List<Object[]> countByPredictedLabel();

    // Risk score phân bổ: buckets 0-25, 25-50, 50-75, 75-100
    @Query("SELECT " +
           "SUM(CASE WHEN p.riskPercentage < 25 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN p.riskPercentage >= 25 AND p.riskPercentage < 50 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN p.riskPercentage >= 50 AND p.riskPercentage < 75 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN p.riskPercentage >= 75 THEN 1 ELSE 0 END) " +
           "FROM ClaimPrediction p WHERE p.riskPercentage IS NOT NULL")
    List<Object[]> countRiskBuckets();

    // ── INVESTIGATOR extras ───────────────────────────────────────
    @Query("SELECT COUNT(DISTINCT p.rawClaim.rawClaimId) FROM ClaimPrediction p WHERE p.shouldAlert = true AND p.rawClaim.investigator.userId = :userId")
    long countAlertsByInvestigator(@Param("userId") Long userId);

    @Query("SELECT COALESCE(AVG(p.riskPercentage), 0) FROM ClaimPrediction p WHERE p.rawClaim.investigator.userId = :userId AND p.riskPercentage IS NOT NULL")
    Double avgRiskByInvestigator(@Param("userId") Long userId);

    @Query("SELECT " +
           "SUM(CASE WHEN p.riskPercentage < 40 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN p.riskPercentage >= 40 AND p.riskPercentage < 70 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN p.riskPercentage >= 70 THEN 1 ELSE 0 END) " +
           "FROM ClaimPrediction p WHERE p.rawClaim.investigator.userId = :userId AND p.riskPercentage IS NOT NULL")
    List<Object[]> countRiskGroupsByInvestigator(@Param("userId") Long userId);

    @Query("SELECT p.rawClaim.rawClaimId, p.riskPercentage FROM ClaimPrediction p " +
           "WHERE p.rawClaim.investigator.userId = :userId " +
           "AND p.rawClaim.claimStatus NOT IN ('APPROVED','REJECTED') " +
           "ORDER BY p.riskPercentage DESC")
    List<Object[]> findTopRiskUnprocessedClaims(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(p) FROM ClaimPrediction p WHERE p.predictedAt >= :startDate AND p.predictedAt <= :endDate")
    long countModelsRunByDateRange(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);
}

