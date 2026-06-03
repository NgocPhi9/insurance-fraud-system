package group102.insurancefraud.repository;

import group102.insurancefraud.entity.ClaimPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
