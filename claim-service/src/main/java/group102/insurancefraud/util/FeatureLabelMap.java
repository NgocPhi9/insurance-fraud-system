package group102.insurancefraud.util;

import java.util.Map;

public final class FeatureLabelMap {

    private static final Map<String, String> LABELS = Map.ofEntries(
        Map.entry("PRVDR_NUM",                  "Mã cơ sở y tế"),           // Provider Institution
        Map.entry("NCH_PRMRY_PYR_CLM_PD_AMT",  "Số tiền bảo hiểm khác chi trả"), // Primary Payer Paid Amount
        Map.entry("AT_PHYSN_NPI",               "Bác sĩ điều trị chính"),     // Attending Physician NPI
        Map.entry("OP_PHYSN_NPI",               "Bác sĩ phẫu thuật"),       // Operating Physician NPI
        Map.entry("OT_PHYSN_NPI",               "Bác sĩ khác"),             // Other Physician NPI
        Map.entry("CLM_UTLZTN_DAY_CNT",         "Số ngày điều trị"),        // Claim Utilization Day Count
        Map.entry("ADMTNG_ICD9_DGNS_CD",        "Mã chẩn đoán nhập viện"),    // Admitting Diagnosis Code (ICD-9)
        Map.entry("CLM_DRG_CD",                 "Mã nhóm chẩn đoán (DRG)"), // Claim DRG Code
        Map.entry("ICD9_PRCDR_CD_1",            "Mã thủ thuật chính"),      // Primary Procedure Code (ICD-9)
        Map.entry("CLM_PMT_AMT",                "Số tiền thanh toán claim"), // Claim Payment Amount
        Map.entry("CLM_FROM_DT",                "Ngày bắt đầu claim"),      // Claims Start Date
        Map.entry("CLM_THRU_DT",                "Ngày kết thúc claim"),     // Claims End Date
        Map.entry("CLM_ADMSN_DT",               "Ngày nhập viện"),          // Inpatient Admission Date
        Map.entry("NCH_BENE_DSCHRG_DT",         "Ngày xuất viện")           // Inpatient Discharged Date
    );

    private FeatureLabelMap() {
        // Prevent instantiation
    }

    public static String getLabel(String technicalName) {
        return LABELS.getOrDefault(technicalName, technicalName);
    }
}
