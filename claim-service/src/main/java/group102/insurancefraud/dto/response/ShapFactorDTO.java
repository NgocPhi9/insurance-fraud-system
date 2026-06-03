package group102.insurancefraud.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShapFactorDTO {

    @JsonProperty("feature")
    private String feature;

    @JsonProperty("impact")
    private Double impact;

    @JsonProperty("direction")
    private String direction;

    @JsonProperty("value")
    private Object value;
}
