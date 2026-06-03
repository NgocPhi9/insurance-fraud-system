package group102.insurancefraud.dto.request;

import group102.insurancefraud.enums.ClaimStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// File: dto/request/CreateClaimRequest.java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClaimRequest {

    @NotBlank(message = "Claim ID không được để trống")
    private String clmId;

    @NotBlank(message = "Provider Number không được để trống")
    private String prvdrNum;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @DecimalMin(value = "0.0", message = "Số tiền phải lớn hơn 0")
    private BigDecimal clmPmtAmt;

    // ML bắt buộc
    @NotNull(message = "Primary Payer Amount không được để trống")
    private BigDecimal nchPrmryPyrClmPdAmt;

    @NotBlank(message = "Attending Physician NPI không được để trống")
    private String atPhysnNpi;

    @NotBlank(message = "Operating Physician NPI không được để trống")
    private String opPhysnNpi;

    @NotBlank(message = "Other Physician NPI không được để trống")
    private String otPhysnNpi;

    @NotNull(message = "Số ngày điều trị không được để trống")
    @Min(value = 0, message = "Số ngày phải >= 0")
    private Integer clmUtlztnDayCnt;

    @NotBlank(message = "Chẩn đoán nhập viện không được để trống")
    private String admtngIcd9DgnsCd;

    @NotBlank(message = "DRG Code không được để trống")
    private String clmDrgCd;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate clmFromDt;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate clmThruDt;

    @NotEmpty(message = "Phải có ít nhất 1 thủ thuật ICD-9")
    private List<String> procedures;

    private String desynpufId;
    private String segment;
    private LocalDate clmAdmsnDt;
    private BigDecimal clmPassThruPerDiemAmt;
    private BigDecimal nchBeneIpDdctblAmt;
    private BigDecimal nchBenePtaCoinsrncLbltyAm;
    private BigDecimal nchBeneBloodDdctblLbltyAm;
    private LocalDate nchBeneDschrgDt;
    private Long claimHandlerId;
    private Long investigatorId;
    private ClaimStatus claimStatus;
    private List<String> diagnoses;
    private List<String> hcpcsCodes;
}