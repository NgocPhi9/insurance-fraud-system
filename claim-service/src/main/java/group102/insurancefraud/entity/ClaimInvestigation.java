package group102.insurancefraud.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CLAIM_INVESTIGATIONS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimInvestigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long investigationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RAW_CLAIM_ID", nullable = false)
    private RawClaim rawClaim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INVESTIGATOR_ID", nullable = false)
    private User investigator;

    @Column(nullable = false)
    private String action;          // "APPROVE" | "REJECT" | "NOTE"

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
