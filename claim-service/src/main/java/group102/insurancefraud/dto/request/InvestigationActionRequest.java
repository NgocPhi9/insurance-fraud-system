package group102.insurancefraud.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestigationActionRequest {

    @NotBlank(message = "Action không được để trống")
    private String action;      // "APPROVE" | "REJECT" | "NOTE"

    private String note;        // bắt buộc nếu REJECT
}
