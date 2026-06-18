package group102.insurancefraud.dto.request;

import group102.insurancefraud.enums.ClaimStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClaimRequest {

    // ──────────────────────────────────────────────────────────────────────────
    // TRƯỜNG BẮT BUỘC
    // ──────────────────────────────────────────────────────────────────────────

    @NotBlank(message = "Mã cơ sở cung cấp dịch vụ không được để trống")
    // Provider Institution
    private String prvdrNum;

    // Ngày nhập viện — bắt buộc, dùng để auto-calc clmFromDt và clmUtlztnDayCnt
    // Inpatient Admission Date
    @NotNull(message = "Ngày nhập viện không được để trống")
    private LocalDate clmAdmsnDt;

    // Ngày xuất viện — bắt buộc, dùng để auto-calc clmThruDt và clmUtlztnDayCnt
    // Inpatient Discharged Date
    @NotNull(message = "Ngày xuất viện không được để trống")
    private LocalDate nchBeneDschrgDt;

    // Mã chẩn đoán nhập viện — bắt buộc cho ML
    // Admitting Diagnosis Code
    @NotBlank(message = "Mã chẩn đoán nhập viện không được để trống")
    private String admtngIcd9DgnsCd;

    // Mã nhóm chẩn đoán DRG — bắt buộc cho ML
    // Claim DRG Code
    @NotBlank(message = "Mã DRG không được để trống")
    private String clmDrgCd;

    // Bác sĩ điều trị chính (Attending) — bắt buộc cho ML
    // Attending Physician NPI
    @NotBlank(message = "Mã NPI bác sĩ điều trị chính không được để trống")
    private String atPhysnNpi;

    // Số tiền bảo hiểm khác chi trả — bắt buộc cho ML (có thể = 0)
    // Primary Payer Paid Amount
    @NotNull(message = "Số tiền bảo hiểm khác chi trả không được để trống")
    @DecimalMin(value = "0.0", message = "Số tiền phải >= 0")
    private BigDecimal nchPrmryPyrClmPdAmt;

    // ──────────────────────────────────────────────────────────────────────────
    // TRƯỜNG TỰ TÍNH (AUTO-CALC) — không bắt buộc, service tự sinh nếu null
    // ──────────────────────────────────────────────────────────────────────────

    // Ngày bắt đầu claim → tự = clmAdmsnDt nếu null
    // Claims Start Date
    private LocalDate clmFromDt;

    // Ngày kết thúc claim → tự = nchBeneDschrgDt nếu null
    // Claims End Date
    private LocalDate clmThruDt;

    // Số ngày điều trị → tự tính từ nchBeneDschrgDt - clmAdmsnDt nếu null
    // Claim Utilization Day Count
    @Min(value = 0, message = "Số ngày phải >= 0")
    private Integer clmUtlztnDayCnt;

    // ──────────────────────────────────────────────────────────────────────────
    // TRƯỜNG TÙY CHỌN (OPTIONAL)
    // ──────────────────────────────────────────────────────────────────────────

    // Mã hồ sơ người thụ hưởng
    // Beneficiary Code
    @NotBlank(message = "Mã hồ sơ người thụ hưởng không được để trống")
    private String desynpufId;

    // Phân đoạn claim → mặc định = "1" nếu null
    // Claim Line Segment
    private String segment;

    // Số tiền Medicare thanh toán
    // Claim Payment Amount
    @NotNull(message = "Số tiền Medicare thanh toán không được để trống")
    @DecimalMin(value = "0.0", message = "Số tiền phải >= 0")
    private BigDecimal clmPmtAmt;

    // Bác sĩ phẫu thuật — bắt buộc nếu có procedures (kiểm tra ở service)
    // Operating Physician NPI
    private String opPhysnNpi;

    // Bác sĩ khác — tùy chọn
    // Other Physician NPI
    private String otPhysnNpi;

    // Các khoản tài chính tùy chọn
    private BigDecimal clmPassThruPerDiemAmt;        // Claim Pass Thru Per Diem Amount
    private BigDecimal nchBeneIpDdctblAmt;            // NCH Beneficiary Inpatient Deductible Amount
    private BigDecimal nchBenePtaCoinsrncLbltyAm;     // NCH Beneficiary Part A Coinsurance Liability Amount
    private BigDecimal nchBeneBloodDdctblLbltyAm;     // NCH Beneficiary Blood Deductible Liability Amount

    // Danh sách thủ thuật ICD-9 (optional, nhưng nếu có → phải có opPhysnNpi)
    // ICD9 Procedure Codes
    private List<String> procedures;

    // Danh sách mã chẩn đoán ICD-9 (optional)
    // ICD9 Diagnosis Codes
    private List<String> diagnoses;

    // Danh sách mã HCPCS (optional)
    // HCPCS Codes
    private List<String> hcpcsCodes;

    // Phân công xử lý (optional, dùng nội bộ)
    private Long claimHandlerId;
    private Long investigatorId;
    private ClaimStatus claimStatus;
}