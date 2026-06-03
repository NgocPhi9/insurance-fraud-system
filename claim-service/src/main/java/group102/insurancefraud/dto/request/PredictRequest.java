package group102.insurancefraud.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictRequest {

    @JsonProperty("PRVDR_NUM")
    private String prvdrNum;

    @JsonProperty("NCH_PRMRY_PYR_CLM_PD_AMT")
    private BigDecimal nchPrmryPyrClmPdAmt;

    @JsonProperty("AT_PHYSN_NPI")
    private String atPhysnNpi;

    @JsonProperty("OP_PHYSN_NPI")
    private String opPhysnNpi;

    @JsonProperty("OT_PHYSN_NPI")
    private String otPhysnNpi;

    @JsonProperty("CLM_UTLZTN_DAY_CNT")
    private Integer clmUtlztnDayCnt;

    @JsonProperty("ADMTNG_ICD9_DGNS_CD")
    private String admtngIcd9DgnsCd;

    @JsonProperty("CLM_DRG_CD")
    private String clmDrgCd;

    @JsonProperty("ICD9_PRCDR_CD_1")
    private String icd9PrcdrCd1;
}