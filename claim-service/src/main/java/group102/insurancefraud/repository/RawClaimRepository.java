package group102.insurancefraud.repository;

import group102.insurancefraud.entity.RawClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RawClaimRepository extends JpaRepository<RawClaim, Long> {
    @Query("SELECT COUNT(r) FROM RawClaim r WHERE r.investigator.userId = :investigatorId " +
            "AND r.claimStatus = 'UNDER_REVIEW'")
    long countActiveClaimsByInvestigator(@Param("investigatorId") Long investigatorId);
}
