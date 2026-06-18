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

    @Query("SELECT FUNCTION('DATE', r.createdAt), COUNT(r) FROM RawClaim r WHERE r.createdAt >= :startDate GROUP BY FUNCTION('DATE', r.createdAt) ORDER BY FUNCTION('DATE', r.createdAt) ASC")
    List<Object[]> countClaimsByDate(@Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT FUNCTION('DATE', r.createdAt), COUNT(r) FROM RawClaim r WHERE r.createdAt >= :startDate AND r.claimHandler.userId = :userId GROUP BY FUNCTION('DATE', r.createdAt) ORDER BY FUNCTION('DATE', r.createdAt) ASC")
    List<Object[]> countClaimsByDateForStaff(@Param("startDate") java.time.LocalDateTime startDate, @Param("userId") Long userId);

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
    @Query("SELECT FUNCTION('DATE', r.createdAt), COUNT(r) FROM RawClaim r " +
           "WHERE r.createdAt >= :startDate AND r.investigator.userId = :userId " +
           "GROUP BY FUNCTION('DATE', r.createdAt) ORDER BY FUNCTION('DATE', r.createdAt) ASC")
    List<Object[]> countClaimsByDateForInvestigator(@Param("startDate") java.time.LocalDateTime startDate,
                                                     @Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.investigator.userId = :userId AND r.investigator IS NOT NULL " +
           "AND r.claimStatus NOT IN ('APPROVED','REJECTED')")
    long countOpenClaimsByInvestigator(@Param("userId") Long userId);

    // ── STAFF extras ──────────────────────────────────────────────
    @Query("SELECT COALESCE(SUM(r.clmPmtAmt), 0) FROM RawClaim r WHERE r.claimHandler.userId = :userId")
    java.math.BigDecimal sumClmPmtAmtByStaff(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.claimHandler.userId = :userId AND r.investigator IS NULL")
    long countUnassignedByStaff(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.claimHandler.userId = :userId AND r.createdAt >= :startDate AND r.createdAt <= :endDate")
    long countClaimsCreatedByDateRange(@Param("userId") Long userId, @Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    // Overdue cases: pending review and older than 'days'
    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.investigator.userId = :userId " +
           "AND r.claimStatus NOT IN ('APPROVED','REJECTED') " +
           "AND r.createdAt < :cutoffDate")
    long countOverdueCasesByInvestigator(@Param("userId") Long userId, @Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    // Avg processing time (in days)
    @Query(value = "SELECT AVG(DATEDIFF(resolved_at, created_at)) FROM raw_claims WHERE resolved_at IS NOT NULL AND claim_handler_id = :userId", nativeQuery = true)
    Double getStaffAvgProcessingTimeDays(@Param("userId") Long userId);

    @Query(value = "SELECT AVG(DATEDIFF(resolved_at, created_at)) FROM raw_claims WHERE resolved_at IS NOT NULL", nativeQuery = true)
    Double getGlobalAvgProcessingTimeDays();

    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.createdAt >= :startDate AND r.createdAt <= :endDate")
    long countClaimsByDateRange(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.claimStatus = 'REJECTED' AND r.createdAt >= :startDate AND r.createdAt <= :endDate")
    long countRejectedClaimsByDateRange(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT FUNCTION('MONTH', r.createdAt), COUNT(r) FROM RawClaim r WHERE r.claimStatus = 'REJECTED' AND r.createdAt >= :startDate GROUP BY FUNCTION('MONTH', r.createdAt) ORDER BY FUNCTION('MONTH', r.createdAt) ASC")
    List<Object[]> countFraudClaimsByMonth(@Param("startDate") java.time.LocalDateTime startDate);

    // ── Filter by status (cho clickable cards) ───────────────────
    org.springframework.data.domain.Page<RawClaim> findByClaimStatus(
            group102.insurancefraud.enums.ClaimStatus status,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<RawClaim> findByInvestigator_UserIdAndClaimStatus(
            Long investigatorId,
            group102.insurancefraud.enums.ClaimStatus status,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<RawClaim> findByClaimHandler_UserIdAndClaimStatus(
            Long handlerId,
            group102.insurancefraud.enums.ClaimStatus status,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r FROM RawClaim r WHERE r.investigator.userId = :userId " +
           "AND r.claimStatus NOT IN ('APPROVED','REJECTED') " +
           "AND r.createdAt < :cutoffDate " +
           "ORDER BY r.createdAt ASC")
    org.springframework.data.domain.Page<RawClaim> findOverdueCasesByInvestigator(
            @Param("userId") Long userId,
            @Param("cutoffDate") java.time.LocalDateTime cutoffDate,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r FROM RawClaim r WHERE r.claimHandler.userId = :userId AND r.investigator IS NULL " +
           "ORDER BY r.createdAt DESC")
    org.springframework.data.domain.Page<RawClaim> findUnassignedByStaff(
            @Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r FROM RawClaim r WHERE r.investigator IS NULL AND r.claimStatus = 'PENDING' " +
           "ORDER BY r.createdAt DESC")
    org.springframework.data.domain.Page<RawClaim> findUnassignedPending(
            org.springframework.data.domain.Pageable pageable);
}
