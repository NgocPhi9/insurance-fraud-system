package group102.insurancefraud.mapper;

import group102.insurancefraud.dto.response.ClaimResponse;
import group102.insurancefraud.dto.request.CreateClaimRequest;
import group102.insurancefraud.entity.RawClaim;
import group102.insurancefraud.entity.RawDiagnosis;
import group102.insurancefraud.entity.RawHcpcs;
import group102.insurancefraud.entity.RawProcedure;
import group102.insurancefraud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClaimMapper {

    private final UserRepository userRepository;

    public RawClaim toEntity(CreateClaimRequest request) {

        RawClaim claim = RawClaim.builder()
                .desynpufId(request.getDesynpufId())
                // clmId không nhận từ request — service sẽ tự sinh (xem RawClaimService.createClaim)
                .segment(request.getSegment())
                .clmFromDt(request.getClmFromDt())
                .clmThruDt(request.getClmThruDt())
                .prvdrNum(request.getPrvdrNum())
                .clmPmtAmt(request.getClmPmtAmt())
                .nchPrmryPyrClmPdAmt(request.getNchPrmryPyrClmPdAmt())
                .atPhysnNpi(request.getAtPhysnNpi())
                .opPhysnNpi(request.getOpPhysnNpi())
                .otPhysnNpi(request.getOtPhysnNpi())
                .clmAdmsnDt(request.getClmAdmsnDt())
                .admtngIcd9DgnsCd(request.getAdmtngIcd9DgnsCd())
                .clmPassThruPerDiemAmt(request.getClmPassThruPerDiemAmt())
                .nchBeneIpDdctblAmt(request.getNchBeneIpDdctblAmt())
                .nchBenePtaCoinsrncLbltyAm(request.getNchBenePtaCoinsrncLbltyAm())
                .nchBeneBloodDdctblLbltyAm(request.getNchBeneBloodDdctblLbltyAm())
                .clmUtlztnDayCnt(request.getClmUtlztnDayCnt())
                .nchBeneDschrgDt(request.getNchBeneDschrgDt())
                .clmDrgCd(request.getClmDrgCd())
                .claimStatus(request.getClaimStatus())
                .build();


        // Map claimHandler
        if (request.getClaimHandlerId() != null) {
            userRepository.findById(request.getClaimHandlerId())
                    .ifPresent(claim::setClaimHandler);
        }

        // Map investigator
        if (request.getInvestigatorId() != null) {
            userRepository.findById(request.getInvestigatorId())
                    .ifPresent(claim::setInvestigator);
        }

        if (request.getDiagnoses() != null) {

            List<RawDiagnosis> diagnoses = request.getDiagnoses()
                    .stream()
                    .map(code -> RawDiagnosis.builder()
                            .icd9DgnsCd(code)
                            .rawClaim(claim)
                            .build())
                    .collect(Collectors.toList());

            claim.setDiagnoses(diagnoses);
        }

        if (request.getProcedures() != null) {

            List<RawProcedure> procedures = request.getProcedures()
                    .stream()
                    .map(code -> RawProcedure.builder()
                            .icd9PrcdrCd(code)
                            .rawClaim(claim)
                            .build())
                    .collect(Collectors.toList());

            claim.setProcedures(procedures);
        }

        if (request.getHcpcsCodes() != null) {

            List<RawHcpcs> hcpcs = request.getHcpcsCodes()
                    .stream()
                    .map(code -> RawHcpcs.builder()
                            .hcpcsCd(code)
                            .rawClaim(claim)
                            .build())
                    .collect(Collectors.toList());

            claim.setHcpcsCodes(hcpcs);
        }

        return claim;
    }

    public ClaimResponse toResponse(RawClaim claim) {

        return ClaimResponse.builder()
                .rawClaimId(claim.getRawClaimId())
                .desynpufId(claim.getDesynpufId())
                .segment(claim.getSegment())
                .clmFromDt(claim.getClmFromDt())
                .clmThruDt(claim.getClmThruDt())
                .prvdrNum(claim.getPrvdrNum())
                .clmPmtAmt(claim.getClmPmtAmt())
                .nchPrmryPyrClmPdAmt(claim.getNchPrmryPyrClmPdAmt())
                .clmPassThruPerDiemAmt(claim.getClmPassThruPerDiemAmt())
                .nchBeneIpDdctblAmt(claim.getNchBeneIpDdctblAmt())
                .nchBenePtaCoinsrncLbltyAm(claim.getNchBenePtaCoinsrncLbltyAm())
                .nchBeneBloodDdctblLbltyAm(claim.getNchBeneBloodDdctblLbltyAm())
                .atPhysnNpi(claim.getAtPhysnNpi())
                .opPhysnNpi(claim.getOpPhysnNpi())
                .otPhysnNpi(claim.getOtPhysnNpi())
                .clmAdmsnDt(claim.getClmAdmsnDt())
                .admtngIcd9DgnsCd(claim.getAdmtngIcd9DgnsCd())
                .clmUtlztnDayCnt(claim.getClmUtlztnDayCnt())
                .nchBeneDschrgDt(claim.getNchBeneDschrgDt())
                .clmDrgCd(claim.getClmDrgCd())
                .claimStatus(claim.getClaimStatus())
                // Diagnoses
                .diagnoses(claim.getDiagnoses() == null ? List.of()
                        : claim.getDiagnoses().stream()
                        .map(RawDiagnosis::getIcd9DgnsCd).toList())
                // Procedures
                .procedures(claim.getProcedures() == null ? List.of()
                        : claim.getProcedures().stream()
                        .map(RawProcedure::getIcd9PrcdrCd).toList())
                // Hcpcs
                .hcpcsCodes(claim.getHcpcsCodes() == null ? List.of()
                        : claim.getHcpcsCodes().stream()
                        .map(RawHcpcs::getHcpcsCd).toList())
                // Handler
                .claimHandlerId(claim.getClaimHandler() != null
                        ? claim.getClaimHandler().getUserId() : null)
                .claimHandlerName(claim.getClaimHandler() != null
                        ? claim.getClaimHandler().getFullName() : null)
                // Investigator
                .investigatorId(claim.getInvestigator() != null
                        ? claim.getInvestigator().getUserId() : null)
                .investigatorName(claim.getInvestigator() != null
                        ? claim.getInvestigator().getFullName() : null)
                .build();
    }
}
