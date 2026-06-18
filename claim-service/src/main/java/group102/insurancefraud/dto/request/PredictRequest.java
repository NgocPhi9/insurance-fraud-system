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

    @JsonProperty("DESYNPUF_ID")
    private String desynpufId;

    @JsonProperty("CLM_ID")
    private String clmId;

    @JsonProperty("SEGMENT")
    private Integer segment;

    @JsonProperty("CLM_FROM_DT")
    private Integer clmFromDt;

    @JsonProperty("CLM_THRU_DT")
    private Integer clmThruDt;

    @JsonProperty("PRVDR_NUM")
    private String prvdrNum;

    @JsonProperty("CLM_PMT_AMT")
    private BigDecimal clmPmtAmt;

    @JsonProperty("NCH_PRMRY_PYR_CLM_PD_AMT")
    private BigDecimal nchPrmryPyrClmPdAmt;

    @JsonProperty("AT_PHYSN_NPI")
    private String atPhysnNpi;

    @JsonProperty("OP_PHYSN_NPI")
    private String opPhysnNpi;

    @JsonProperty("OT_PHYSN_NPI")
    private String otPhysnNpi;

    @JsonProperty("CLM_ADMSN_DT")
    private Integer clmAdmsnDt;

    @JsonProperty("ADMTNG_ICD9_DGNS_CD")
    private String admtngIcd9DgnsCd;

    @JsonProperty("CLM_PASS_THRU_PER_DIEM_AMT")
    private BigDecimal clmPassThruPerDiemAmt;

    @JsonProperty("NCH_BENE_IP_DDCTBL_AMT")
    private BigDecimal nchBeneIpDdctblAmt;

    @JsonProperty("NCH_BENE_PTA_COINSRNC_LBLTY_AM")
    private BigDecimal nchBenePtaCoinsrncLbltyAm;

    @JsonProperty("NCH_BENE_BLOOD_DDCTBL_LBLTY_AM")
    private BigDecimal nchBeneBloodDdctblLbltyAm;

    @JsonProperty("CLM_UTLZTN_DAY_CNT")
    private Integer clmUtlztnDayCnt;

    @JsonProperty("NCH_BENE_DSCHRG_DT")
    private Integer nchBeneDschrgDt;

    @JsonProperty("CLM_DRG_CD")
    private String clmDrgCd;

    @JsonProperty("ICD9_DGNS_CD_1")
    private String icd9DgnsCd1;

    @JsonProperty("ICD9_PRCDR_CD_1")
    private String icd9PrcdrCd1;
}