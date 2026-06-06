package group102.insurancefraud.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudSuspectDto {
    private String entityId;
    private long fraudCount;
    private BigDecimal totalAmountAtRisk;
    private String riskLevel; // Very High, High, Medium, Low
}
