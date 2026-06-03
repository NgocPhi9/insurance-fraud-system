package group102.insurancefraud.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestigationResponse {

    private Long investigationId;
    private Long rawClaimId;
    private String investigatorName;
    private String action;
    private String note;
    private LocalDateTime createdAt;
}