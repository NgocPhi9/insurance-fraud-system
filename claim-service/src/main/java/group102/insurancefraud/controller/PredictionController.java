package group102.insurancefraud.controller;

import group102.insurancefraud.dto.response.ApiResponse;
import group102.insurancefraud.dto.response.PredictionResponse;
import group102.insurancefraud.service.PredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
@Slf4j
public class PredictionController {

    private final PredictionService predictionService;

    @PostMapping("/predict/{rawClaimId}")
    public ResponseEntity<ApiResponse<PredictionResponse>> predictManual(
            @PathVariable Long rawClaimId) {

        log.info("POST /predict/{}", rawClaimId);
        PredictionResponse result = predictionService.predictManual(rawClaimId);
        return ResponseEntity.ok(ApiResponse.<PredictionResponse>builder()
                .success(true)
                .message("OK")
                .data(result)
                .build());
    }

    @PostMapping("/predict/batch")
    public ResponseEntity<ApiResponse<Void>> predictBatch(
            @RequestBody List<Long> rawClaimIds) {

        log.info("POST /predict/batch, size: {}", rawClaimIds.size());
        predictionService.predictBatchAsync(rawClaimIds);   // async, không trả data
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Batch predict đang xử lý")
                .data(null)
                .build());
    }

    @GetMapping("/{rawClaimId}/latest")
    public ResponseEntity<ApiResponse<PredictionResponse>> getLatest(
            @PathVariable Long rawClaimId) {

        log.info("GET /predictions/{}/latest", rawClaimId);
        try {
            PredictionResponse result = predictionService.getLatestByClaimId(rawClaimId);
            return ResponseEntity.ok(ApiResponse.<PredictionResponse>builder()
                    .success(true)
                    .message("OK")
                    .data(result)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.<PredictionResponse>builder()
                    .success(true)
                    .message("PENDING")
                    .data(null)
                    .build());
        }
    }

    @GetMapping("/{rawClaimId}/all")
    public ResponseEntity<ApiResponse<List<PredictionResponse>>> getAll(
            @PathVariable Long rawClaimId) {

        log.info("GET /predictions/{}/all", rawClaimId);
        List<PredictionResponse> results = predictionService.getAllByClaimId(rawClaimId);
        return ResponseEntity.ok(ApiResponse.<List<PredictionResponse>>builder()
                .success(true)
                .message("OK")
                .data(results)
                .build());
    }
}