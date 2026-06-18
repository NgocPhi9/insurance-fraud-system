package group102.insurancefraud.entity;

import group102.insurancefraud.enums.ClaimStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "RAW_CLAIMS")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RawClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rawClaimId;

    private String desynpufId;

    private String segment;

    private LocalDate clmFromDt;

    private LocalDate clmThruDt;

    private String prvdrNum;

    private BigDecimal clmPmtAmt;
    private BigDecimal nchPrmryPyrClmPdAmt;

    private String atPhysnNpi;
    private String opPhysnNpi;
    private String otPhysnNpi;

    private LocalDate clmAdmsnDt;

    @Column(name = "ADMTNG_ICD9_DGNS_CD")
    private String admtngIcd9DgnsCd;

    private BigDecimal clmPassThruPerDiemAmt;
    private BigDecimal nchBeneIpDdctblAmt;
    private BigDecimal nchBenePtaCoinsrncLbltyAm;
    private BigDecimal nchBeneBloodDdctblLbltyAm;

    private Integer clmUtlztnDayCnt;

    private LocalDate nchBeneDschrgDt;

    private String clmDrgCd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLAIM_HANDLER_ID")
    private User claimHandler;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INVESTIGATOR_ID")
    private User investigator;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ClaimStatus claimStatus = ClaimStatus.PENDING;

    @OneToMany(mappedBy = "rawClaim", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<RawDiagnosis> diagnoses;

    @OneToMany(mappedBy = "rawClaim", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<RawProcedure> procedures;

    @OneToMany(mappedBy = "rawClaim", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<RawHcpcs> hcpcsCodes;

    @OneToMany(mappedBy = "rawClaim", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ClaimPrediction> predictions;

    @Version
    private Long version;
}
