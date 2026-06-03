package group102.insurancefraud.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "CLAIM_PREDICTIONS")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClaimPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long predictionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RAW_CLAIM_ID", nullable = false)
    private RawClaim rawClaim;

    private String modelName;
    private String modelVersion;

    private String predictedLabel;

    @Column(columnDefinition = "DECIMAL(10,4)")
    private Double anomalyScore;

    @Column(columnDefinition = "DECIMAL(10,4)")
    private Double riskPercentage;

    private Boolean shouldAlert;

    @Column(length = 10000)
    private String shapSummary;

    private Integer shapConfidence;

    private String shapMethod;

    private LocalDateTime predictedAt;

    @OneToMany(mappedBy = "claimPrediction", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ClaimShapFactor> shapFactors;
}
