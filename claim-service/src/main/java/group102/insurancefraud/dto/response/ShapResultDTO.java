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
public class ShapResultDTO {

    // Map từ ML /shap response
    @JsonProperty("top_factors")
    private List<ShapFactorDTO> topFactors;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("confidence")
    private Double confidence;

    @JsonProperty("method")
    private String method;

    @JsonProperty("provider_id")
    private String providerId;
}