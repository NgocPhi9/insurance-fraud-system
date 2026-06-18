package group102.insurancefraud.service;

import group102.insurancefraud.dto.request.PredictRequest;
import group102.insurancefraud.dto.response.AnomalyResultDTO;
import group102.insurancefraud.dto.response.PredictionResponse;
import group102.insurancefraud.dto.response.ShapResultDTO;
import group102.insurancefraud.entity.ClaimPrediction;
import group102.insurancefraud.entity.RawClaim;
import group102.insurancefraud.entity.RawProcedure;
import group102.insurancefraud.enums.ClaimStatus;
import group102.insurancefraud.mapper.PredictionMapper;
import group102.insurancefraud.repository.ClaimPredictionRepository;
import group102.insurancefraud.repository.RawClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PredictionService {

    private final RawClaimRepository rawClaimRepository;
    private final ClaimPredictionRepository claimPredictionRepository;
    private final MlClientService mlClientService;
    private final PredictionMapper predictionMapper;

    // ── Async: tự động chạy sau khi tạo claim ────────────────────
    @Async
    @Transactional
    public void predictAsync(Long rawClaimId) {
        try {
            log.info("Async predict started for claimId: {}", rawClaimId);
            RawClaim rawClaim = rawClaimRepository.findById(rawClaimId)
                    .orElseThrow(() -> new RuntimeException("Claim not found: " + rawClaimId));

            List<RawProcedure> procedures = rawClaim.getProcedures() != null
                    ? rawClaim.getProcedures() : new ArrayList<>();

            PredictRequest mlRequest = predictionMapper.toMlRequest(rawClaim, procedures);
            AnomalyResultDTO anomalyResult = mlClientService.predict(mlRequest);
            ShapResultDTO shapResult = mlClientService.getShap(mlRequest);

            ClaimPrediction prediction = predictionMapper.toEntity(anomalyResult, rawClaim);
            predictionMapper.enrichWithShap(prediction, shapResult);
            claimPredictionRepository.save(prediction);

            // ── Cập nhật status claim sau khi ML xong ──
            updateClaimStatusAfterML(rawClaim, anomalyResult);
            log.info("Claim [{}] status updated to [{}]",
                    rawClaimId, rawClaim.getClaimStatus());
            rawClaimRepository.save(rawClaim);

            log.info("Claim [{}] status updated to UNDER_REVIEW", rawClaimId);
        } catch (Exception e) {
            log.error("Async predict failed for claimId: {}, error: {}", rawClaimId, e.getMessage());
        }
    }


    // ── Manual: nhấn nút predict lại ─────────────────────────────
    @Transactional
    public PredictionResponse predictManual(Long rawClaimId) {
        RawClaim rawClaim = rawClaimRepository.findById(rawClaimId)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + rawClaimId));

        List<RawProcedure> procedures = rawClaim.getProcedures() != null
                ? rawClaim.getProcedures() : new ArrayList<>();

        PredictRequest mlRequest = predictionMapper.toMlRequest(rawClaim, procedures);
        AnomalyResultDTO anomalyResult = mlClientService.predict(mlRequest);
        ShapResultDTO shapResult = mlClientService.getShap(mlRequest);

        ClaimPrediction prediction = predictionMapper.toEntity(anomalyResult, rawClaim);
        predictionMapper.enrichWithShap(prediction, shapResult);
        claimPredictionRepository.save(prediction);

        // ── Cập nhật status ──
        updateClaimStatusAfterML(rawClaim, anomalyResult);
        rawClaimRepository.save(rawClaim);

        return predictionMapper.toResponse(prediction);
    }

    // ── Batch async ───────────────────────────────────────────────
    @Async
    @Transactional
    public void predictBatchAsync(List<Long> rawClaimIds) {
        List<RawClaim> rawClaims = rawClaimRepository.findAllById(rawClaimIds);
        if (rawClaims.isEmpty()) return;

        List<PredictRequest> mlRequests = rawClaims.stream()
                .map(claim -> predictionMapper.toMlRequest(
                        claim,
                        claim.getProcedures() != null ? claim.getProcedures() : new ArrayList<>()
                ))
                .collect(Collectors.toList());

        List<AnomalyResultDTO> anomalyResults = mlClientService.predictBatch(mlRequests);

        for (int i = 0; i < rawClaims.size(); i++) {
            try {
                RawClaim rawClaim = rawClaims.get(i);
                AnomalyResultDTO anomalyResult = anomalyResults.get(i);
                PredictRequest mlRequest = mlRequests.get(i);

                ShapResultDTO shapResult = mlClientService.getShap(mlRequest);

                ClaimPrediction prediction = predictionMapper.toEntity(anomalyResult, rawClaim);
                predictionMapper.enrichWithShap(prediction, shapResult);
                claimPredictionRepository.save(prediction);

                updateClaimStatusAfterML(rawClaim, anomalyResult);
                rawClaimRepository.save(rawClaim);

                log.info("Batch predict done for claimId: {}", rawClaim.getRawClaimId());
            } catch (Exception e) {
                log.error("Batch predict failed at index {}: {}", i, e.getMessage());
            }
        }
    }

    // ── Query ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PredictionResponse getLatestByClaimId(Long rawClaimId) {
        ClaimPrediction prediction = claimPredictionRepository
                .findTopByRawClaim_RawClaimIdOrderByPredictedAtDesc(rawClaimId)
                .orElseThrow(() -> new RuntimeException("No prediction found for claim: " + rawClaimId));
        return predictionMapper.toResponse(prediction);
    }

    @Transactional(readOnly = true)
    public List<PredictionResponse> getAllByClaimId(Long rawClaimId) {
        return claimPredictionRepository
                .findByRawClaim_RawClaimId(rawClaimId)
                .stream()
                .map(predictionMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void updateClaimStatusAfterML(RawClaim rawClaim, AnomalyResultDTO result) {
        ClaimStatus currentStatus = rawClaim.getClaimStatus();

        if (currentStatus == ClaimStatus.PENDING || currentStatus == ClaimStatus.FLAGGED) {
            if ("ANOMALY".equals(result.getPrediction())) {
                // ML phát hiện bất thường → FLAGGED, chờ investigator nhận
                rawClaim.setClaimStatus(ClaimStatus.FLAGGED);
            } else {
                // ML cho là bình thường → APPROVED luôn
                rawClaim.setClaimStatus(ClaimStatus.APPROVED);
                rawClaim.setResolvedAt(LocalDateTime.now());
            }
            rawClaimRepository.save(rawClaim);
        } else {
            log.info("Claim [{}] đang ở trạng thái [{}]. Giữ nguyên trạng thái sau khi phân tích lại.",
                     rawClaim.getRawClaimId(), currentStatus);
        }
    }
}