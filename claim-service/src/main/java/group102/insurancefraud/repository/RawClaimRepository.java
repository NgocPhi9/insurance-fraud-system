package group102.insurancefraud.repository;

import group102.insurancefraud.entity.RawClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawClaimRepository extends JpaRepository<RawClaim, Long> {
    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.investigator.userId = :investigatorId " +
            "AND r.claimStatus = 'UNDER_REVIEW'")
    long countActiveClaimsByInvestigator(@Param("investigatorId") Long investigatorId);

    List<RawClaim> findByClaimHandler_UserId(Long userId);

    org.springframework.data.domain.Page<RawClaim> findByClaimHandler_UserId(Long userId, org.springframework.data.domain.Pageable pageable);

    List<RawClaim> findByInvestigator_UserId(Long userId);

    org.springframework.data.domain.Page<RawClaim> findByInvestigator_UserId(Long userId, org.springframework.data.domain.Pageable pageable);

    List<RawClaim> findByDesynpufIdContainingIgnoreCase(String desynpufId);

    List<RawClaim> findByPrvdrNumContainingIgnoreCase(String prvdrNum);

    @Query("SELECT r.desynpufId, COUNT(r), COALESCE(SUM(r.clmPmtAmt), 0) " +
           "FROM RawClaim r " +
           "WHERE r.claimStatus = 'REJECTED' AND r.desynpufId IS NOT NULL " +
           "GROUP BY r.desynpufId " +
           "HAVING COUNT(r) >= :minFraudClaims " +
           "ORDER BY COUNT(r) DESC")
    List<Object[]> findTopFraudulentBeneficiaries(@Param("minFraudClaims") long minFraudClaims, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r.prvdrNum, COUNT(r), COALESCE(SUM(r.clmPmtAmt), 0) " +
           "FROM RawClaim r " +
           "WHERE r.claimStatus = 'REJECTED' AND r.prvdrNum IS NOT NULL " +
           "GROUP BY r.prvdrNum " +
           "HAVING COUNT(r) >= :minFraudClaims " +
           "ORDER BY COUNT(r) DESC")
    List<Object[]> findTopFraudulentProviders(@Param("minFraudClaims") long minFraudClaims, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(DISTINCT r.desynpufId) FROM RawClaim r WHERE r.claimStatus = 'REJECTED' AND r.desynpufId IS NOT NULL")
    long countUniqueFraudulentBeneficiaries();

    @Query("SELECT COUNT(DISTINCT r.prvdrNum) FROM RawClaim r WHERE r.claimStatus = 'REJECTED' AND r.prvdrNum IS NOT NULL")
    long countUniqueFraudulentProviders();

    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.claimStatus = 'REJECTED'")
    long countTotalRejectedClaims();

    @Query("SELECT COALESCE(SUM(r.clmPmtAmt), 0) FROM RawClaim r WHERE r.claimStatus = 'REJECTED'")
    java.math.BigDecimal sumTotalRejectedAmount();
    @Query("SELECT r.claimStatus, COUNT(r) FROM RawClaim r GROUP BY r.claimStatus")
    List<Object[]> countClaimsByStatus();

    @Query("SELECT r.claimStatus, COUNT(r) FROM RawClaim r WHERE r.claimHandler.userId = :userId GROUP BY r.claimStatus")
    List<Object[]> countClaimsByStatusForStaff(@Param("userId") Long userId);

    @Query("SELECT r.claimStatus, COUNT(r) FROM RawClaim r WHERE r.investigator.userId = :userId GROUP BY r.claimStatus")
    List<Object[]> countClaimsByStatusForInvestigator(@Param("userId") Long userId);

    @Query("SELECT r.clmFromDt, COUNT(r) FROM RawClaim r WHERE r.clmFromDt >= :startDate GROUP BY r.clmFromDt ORDER BY r.clmFromDt ASC")
    List<Object[]> countClaimsByDate(@Param("startDate") java.time.LocalDate startDate);

    @Query("SELECT r.clmFromDt, COUNT(r) FROM RawClaim r WHERE r.clmFromDt >= :startDate AND r.claimHandler.userId = :userId GROUP BY r.clmFromDt ORDER BY r.clmFromDt ASC")
    List<Object[]> countClaimsByDateForStaff(@Param("startDate") java.time.LocalDate startDate, @Param("userId") Long userId);

    long countByInvestigator_UserId(Long userId);
    
    long countByInvestigator_UserIdAndClaimStatus(Long userId, group102.insurancefraud.enums.ClaimStatus claimStatus);

    long countByClaimHandler_UserId(Long userId);
    
    long countByClaimHandler_UserIdAndClaimStatus(Long userId, group102.insurancefraud.enums.ClaimStatus claimStatus);

    // ── ADMIN extras ─────────────────────────────────────────────
    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.claimStatus = 'PENDING'")
    long countPendingClaims();

    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.investigator IS NULL AND r.claimStatus = 'PENDING'")
    long countUnassignedClaims();

    // Top fraud providers (admin)
    @Query("SELECT r.prvdrNum, COUNT(r) FROM RawClaim r WHERE r.claimStatus = 'REJECTED' AND r.prvdrNum IS NOT NULL " +
           "GROUP BY r.prvdrNum ORDER BY COUNT(r) DESC")
    List<Object[]> findTopFraudProvidersSummary(org.springframework.data.domain.Pageable pageable);

    // ── INVESTIGATOR extras ───────────────────────────────────────
    @Query("SELECT r.clmFromDt, COUNT(r) FROM RawClaim r " +
           "WHERE r.clmFromDt >= :startDate AND r.investigator.userId = :userId " +
           "GROUP BY r.clmFromDt ORDER BY r.clmFromDt ASC")
    List<Object[]> countClaimsByDateForInvestigator(@Param("startDate") java.time.LocalDate startDate,
                                                     @Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.investigator.userId = :userId AND r.investigator IS NOT NULL " +
           "AND r.claimStatus NOT IN ('APPROVED','REJECTED')")
    long countOpenClaimsByInvestigator(@Param("userId") Long userId);

    // ── STAFF extras ──────────────────────────────────────────────
    @Query("SELECT COALESCE(SUM(r.clmPmtAmt), 0) FROM RawClaim r WHERE r.claimHandler.userId = :userId")
    java.math.BigDecimal sumClmPmtAmtByStaff(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.claimHandler.userId = :userId AND r.investigator IS NULL")
    long countUnassignedByStaff(@Param("userId") Long userId);
}

