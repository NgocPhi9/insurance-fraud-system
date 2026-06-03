package group102.insurancefraud.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "RAW_DIAGNOSES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawDiagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diagnosisId;

    @Column(name = "ICD9_DGNS_CD")
    private String icd9DgnsCd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RAW_CLAIM_ID", nullable = false)
    @ToString.Exclude
    private RawClaim rawClaim;
}
