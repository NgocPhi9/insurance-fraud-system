package group102.insurancefraud.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetrainResponseDTO {
    @JsonProperty("baseline_version")
    private String baselineVersion;

    @JsonProperty("retrains")
    private List<RetrainDetailDTO> retrains;
}
