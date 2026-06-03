package group102.insurancefraud.enums;

public enum ClaimStatus {
    PENDING,        // Mới tạo, chờ ML phân tích
    FLAGGED,        // ML xong → anomaly, chờ investigator nhận
    UNDER_REVIEW,   // Investigator đã nhận, đang điều tra
    APPROVED,       // Duyệt hợp lệ
    REJECTED        // Xác nhận gian lận
}