package group102.insurancefraud.service;

import group102.insurancefraud.dto.response.RetrainDetailDTO;
import group102.insurancefraud.dto.response.RetrainResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class RetrainStateService {

    private final MlClientService mlClientService;

    private boolean isRetraining = false;
    private String versionBeforeRetrain = null;
    private LocalDateTime retrainStartTime = null;

    // Timeout is 30 minutes
    private static final int TIMEOUT_MINUTES = 30;

    public synchronized void startRetrain(String currentVersion) {
        this.isRetraining = true;
        this.versionBeforeRetrain = currentVersion;
        this.retrainStartTime = LocalDateTime.now();
    }

    public synchronized boolean isRetrainingInProgress() {
        if (!isRetraining) {
            return false;
        }

        // Check timeout
        if (retrainStartTime != null && retrainStartTime.plusMinutes(TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
            log.warn("Retrain state timed out, resetting to false.");
            isRetraining = false;
            return false;
        }

        // Fetch current model history to check if a new version appeared
        try {
            RetrainResponseDTO history = mlClientService.getRetrainInfo();
            if (history != null && history.getRetrains() != null && !history.getRetrains().isEmpty()) {
                // Get the latest version from history
                RetrainDetailDTO latestRetrain = history.getRetrains().get(history.getRetrains().size() - 1);
                String latestVersion = latestRetrain.getVersion();
                if (versionBeforeRetrain == null || !versionBeforeRetrain.equals(latestVersion)) {
                    // A new version has appeared, but we need to ensure it's actually finished
                    // The outputs dir is created early, so we check if all evaluation images exist
                    if (latestRetrain.getRocCurvesUrl() != null &&
                        latestRetrain.getPrCurvesUrl() != null &&
                        latestRetrain.getConfusionMatricesUrl() != null &&
                        latestRetrain.getScoreDistributionsUrl() != null) {
                        log.info("New model version detected and finished training: {}, retraining finished.", latestVersion);
                        isRetraining = false;
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not fetch model history to check retrain status", e);
        }

        return true;
    }
}
