package group102.insurancefraud.dto.response;

import group102.insurancefraud.enums.ClaimStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimResponse {

    private Long rawClaimId;
    private String desynpufId;
    private String clmId;
    private String segment;
    private LocalDate clmFromDt;
    private LocalDate clmThruDt;
    private String prvdrNum;

    private BigDecimal clmPmtAmt;
    private BigDecimal nchPrmryPyrClmPdAmt;
    private BigDecimal clmPassThruPerDiemAmt;
    private BigDecimal nchBeneIpDdctblAmt;
    private BigDecimal nchBenePtaCoinsrncLbltyAm;
    private BigDecimal nchBeneBloodDdctblLbltyAm;

    private String atPhysnNpi;
    private String opPhysnNpi;
    private String otPhysnNpi;

    private LocalDate clmAdmsnDt;
    private String admtngIcd9DgnsCd;
    private Integer clmUtlztnDayCnt;
    private LocalDate nchBeneDschrgDt;
    private String clmDrgCd;

    private ClaimStatus claimStatus;

    private List<String> diagnoses;
    private List<String> procedures;
    private List<String> hcpcsCodes;

    private Long claimHandlerId;
    private String claimHandlerName;
    private Long investigatorId;
    private String investigatorName;
}