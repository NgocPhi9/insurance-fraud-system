package group102.insurancefraud.service;

import group102.insurancefraud.dto.response.RetrainDetailDTO;
import group102.insurancefraud.dto.response.RetrainResponseDTO;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class RetrainStatusService {

    private static final Duration RETRAIN_TIMEOUT = Duration.ofHours(1);

    private boolean inProgress;
    private String baselineLatestTimestamp;

    @Getter
    private Instant startedAt;

    public synchronized boolean isInProgress(RetrainResponseDTO currentInfo) {
        refresh(currentInfo);
        return inProgress;
    }

    public synchronized boolean beginIfIdle(RetrainResponseDTO currentInfo) {
        refresh(currentInfo);
        if (inProgress) {
            return false;
        }

        baselineLatestTimestamp = latestTimestamp(currentInfo);
        startedAt = Instant.now();
        inProgress = true;
        return true;
    }

    public synchronized void clear() {
        inProgress = false;
        baselineLatestTimestamp = null;
        startedAt = null;
    }

    private void refresh(RetrainResponseDTO currentInfo) {
        if (!inProgress) {
            return;
        }

        if (startedAt != null && Duration.between(startedAt, Instant.now()).compareTo(RETRAIN_TIMEOUT) > 0) {
            clear();
            return;
        }

        String latestTimestamp = latestTimestamp(currentInfo);
        if (latestTimestamp != null && !latestTimestamp.equals(baselineLatestTimestamp)) {
            clear();
        }
    }

    private String latestTimestamp(RetrainResponseDTO currentInfo) {
        if (currentInfo == null) {
            return null;
        }

        List<RetrainDetailDTO> retrains = currentInfo.getRetrains();
        if (retrains == null || retrains.isEmpty()) {
            return null;
        }

        return retrains.stream()
                .map(RetrainDetailDTO::getTimestamp)
                .filter(timestamp -> timestamp != null && !timestamp.isBlank())
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
