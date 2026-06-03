package group102.insurancefraud.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "RAW_HCPCS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawHcpcs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hcpcsId;

    private String hcpcsCd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RAW_CLAIM_ID", nullable = false)
    @ToString.Exclude
    private RawClaim rawClaim;
}
