package group102.insurancefraud.repository;

import group102.insurancefraud.entity.ClaimInvestigation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClaimInvestigationRepository extends JpaRepository<ClaimInvestigation, Long> {

    List<ClaimInvestigation> findByRawClaim_RawClaimIdOrderByCreatedAtDesc(Long rawClaimId);
}
