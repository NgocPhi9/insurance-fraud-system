package group102.insurancefraud.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResponse {

    private Long predictionId;
    private Long rawClaimId;
    private String predictedLabel;
    private Double anomalyScore;
    private Double riskPercentage;
    private Boolean shouldAlert;
    private String shapSummary;
    private Integer shapConfidence;
    private String shapMethod;
    private List<ShapFactorDTO> topFactors;
    private LocalDateTime predictedAt;
}
