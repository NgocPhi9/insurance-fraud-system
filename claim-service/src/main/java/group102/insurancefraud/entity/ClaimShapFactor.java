package group102.insurancefraud.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CLAIM_SHAP_FACTORS")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClaimShapFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PREDICTION_ID", nullable = false)
    private ClaimPrediction claimPrediction;

    private String featureName;
    private String featureValue;


    @Column(columnDefinition = "DECIMAL(10,4)")
    private Double shapImpact;

    private String direction;
}
