package group102.insurancefraud.service;

import group102.insurancefraud.dto.request.PredictRequest;
import group102.insurancefraud.dto.response.AnomalyResultDTO;
import group102.insurancefraud.dto.response.RetrainResponseDTO;
import group102.insurancefraud.dto.response.RetrainStatusResponseDTO;
import group102.insurancefraud.dto.response.ShapResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MlClientService {

    private final WebClient mlWebClient;

    public AnomalyResultDTO predict(PredictRequest request) {
        try {
            return mlWebClient.post()
                    .uri("/predict")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AnomalyResultDTO.class)
                    .block();                          // blocking — phù hợp flow tuần tự
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("ML /predict failed: {} - Body: {}", e.getMessage(), e.getResponseBodyAsString());
            throw new RuntimeException("ML predict service unavailable");
        } catch (Exception e) {
            log.error("ML /predict failed: {}", e.getMessage());
            throw new RuntimeException("ML predict service unavailable");
        }
    }

    public List<AnomalyResultDTO> predictBatch(List<PredictRequest> requests) {
        try {
            AnomalyResultDTO[] results = mlWebClient.post()
                    .uri("/predict_batch")
                    .bodyValue(requests)
                    .retrieve()
                    .bodyToMono(AnomalyResultDTO[].class)
                    .block();
            assert results != null;
            return Arrays.asList(results);
        } catch (Exception e) {
            log.error("ML /predict_batch failed, size: {}", requests.size());
            throw new RuntimeException("ML batch predict service unavailable");
        }
    }

    public ShapResultDTO getShap(PredictRequest request) {
        try {
            return mlWebClient.post()
                    .uri("/explain")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ShapResultDTO.class)
                    .block();
        } catch (Exception e) {
            log.error("ML /explain failed: {}", e.getMessage());
            throw new RuntimeException("ML shap service unavailable");
        }
    }

    public RetrainResponseDTO getRetrainInfo() {
        try {
            return mlWebClient.get()
                    .uri("/model/history")
                    .retrieve()
                    .bodyToMono(RetrainResponseDTO.class)
                    .block();
        } catch (Exception e) {
            log.error("ML GET /model/history failed: {}", e.getMessage());
            return null;
        }
    }

    public boolean getRetrainStatus() {
        try {
            RetrainStatusResponseDTO response = mlWebClient.get()
                    .uri("/retrain/status")
                    .retrieve()
                    .bodyToMono(RetrainStatusResponseDTO.class)
                    .block();
            return response != null && response.isRetraining();
        } catch (Exception e) {
            log.error("ML GET /retrain/status failed: {}", e.getMessage());
            return false;
        }
    }

    public RetrainResponseDTO retrain() {
        try {
            return mlWebClient.post()
                    .uri("/retrain")
                    .retrieve()
                    .bodyToMono(RetrainResponseDTO.class)
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("ML /retrain failed: {} - Body: {}", e.getMessage(), e.getResponseBodyAsString());
            throw new RuntimeException("ML retrain service failed");
        } catch (Exception e) {
            log.error("ML /retrain failed: {}", e.getMessage());
            throw new RuntimeException("ML retrain service unavailable");
        }
    }
}
