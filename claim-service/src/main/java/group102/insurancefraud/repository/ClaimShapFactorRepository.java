package group102.insurancefraud.repository;

import group102.insurancefraud.entity.ClaimShapFactor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimShapFactorRepository extends JpaRepository<ClaimShapFactor, Long> {

    // ── Toàn hệ thống (dùng cho ADMIN) ──────────────────────────────────────

    // Top features hay bị gắn cờ nhất (theo tần suất) – claims FLAGGED hoặc REJECTED
    @Query("SELECT s.featureName, COUNT(s) " +
           "FROM ClaimShapFactor s " +
           "WHERE s.claimPrediction.rawClaim.claimStatus IN ('FLAGGED', 'REJECTED') " +
           "GROUP BY s.featureName " +
           "ORDER BY COUNT(s) DESC")
    List<Object[]> findTopFeaturesByFrequency(Pageable pageable);

    // Top features có tác động cao nhất (theo trung bình absolute shapImpact) – claims FLAGGED hoặc REJECTED
    @Query("SELECT s.featureName, AVG(ABS(s.shapImpact)) " +
           "FROM ClaimShapFactor s " +
           "WHERE s.claimPrediction.rawClaim.claimStatus IN ('FLAGGED', 'REJECTED') " +
           "GROUP BY s.featureName " +
           "ORDER BY AVG(ABS(s.shapImpact)) DESC")
    List<Object[]> findTopFeaturesByImpact(Pageable pageable);

    // ── Scoped theo Investigator (dùng cho INVESTIGATOR dashboard) ───────────

    // Top features hay bị gắn cờ nhất trong các claims được giao cho investigator
    @Query("SELECT s.featureName, COUNT(s) " +
           "FROM ClaimShapFactor s " +
           "WHERE s.claimPrediction.rawClaim.investigator.userId = :investigatorId " +
           "  AND s.claimPrediction.rawClaim.claimStatus IN ('FLAGGED', 'REJECTED') " +
           "GROUP BY s.featureName " +
           "ORDER BY COUNT(s) DESC")
    List<Object[]> findTopFeaturesByFrequencyForInvestigator(@Param("investigatorId") Long investigatorId, Pageable pageable);

    // Top features có tác động cao nhất trong các claims được giao cho investigator
    @Query("SELECT s.featureName, AVG(ABS(s.shapImpact)) " +
           "FROM ClaimShapFactor s " +
           "WHERE s.claimPrediction.rawClaim.investigator.userId = :investigatorId " +
           "  AND s.claimPrediction.rawClaim.claimStatus IN ('FLAGGED', 'REJECTED') " +
           "GROUP BY s.featureName " +
           "ORDER BY AVG(ABS(s.shapImpact)) DESC")
    List<Object[]> findTopFeaturesByImpactForInvestigator(@Param("investigatorId") Long investigatorId, Pageable pageable);
}
