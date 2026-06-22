package group102.insurancefraud.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RetrainStatusResponseDTO {
    @JsonProperty("is_retraining")
    private boolean isRetraining;
}
