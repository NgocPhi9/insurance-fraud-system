package group102.insurancefraud.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyResultDTO {

    @JsonProperty("anomaly_score")
    private Double anomalyScore;

    @JsonProperty("risk_percentage")
    private Double riskPercentage;

    @JsonProperty("prediction")
    private String prediction;

    @JsonProperty("should_alert")
    private Boolean shouldAlert;

    @JsonProperty("provider_id")
    private String providerId;

    @JsonProperty("model_selected")
    private String modelSelected;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
