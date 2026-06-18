package group102.insurancefraud.mapper;

import group102.insurancefraud.dto.request.PredictRequest;
import group102.insurancefraud.dto.response.AnomalyResultDTO;
import group102.insurancefraud.dto.response.PredictionResponse;
import group102.insurancefraud.dto.response.ShapFactorDTO;
import group102.insurancefraud.dto.response.ShapResultDTO;
import group102.insurancefraud.entity.ClaimPrediction;
import group102.insurancefraud.entity.ClaimShapFactor;
import group102.insurancefraud.entity.RawClaim;
import group102.insurancefraud.entity.RawProcedure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PredictionMapper {

    // RawClaim → PredictRequest (gửi lên ML)
    public PredictRequest toMlRequest(RawClaim rawClaim, List<RawProcedure> procedures) {
        String icd9PrcdrCd1 = procedures.isEmpty()
                ? null
                : procedures.getFirst().getIcd9PrcdrCd();

        String icd9DgnsCd1 = rawClaim.getDiagnoses() != null && !rawClaim.getDiagnoses().isEmpty()
                ? rawClaim.getDiagnoses().getFirst().getIcd9DgnsCd()
                : null;

        Integer clmFromDt = rawClaim.getClmFromDt() != null
                ? Integer.parseInt(rawClaim.getClmFromDt().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE))
                : null;
        Integer clmThruDt = rawClaim.getClmThruDt() != null
                ? Integer.parseInt(rawClaim.getClmThruDt().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE))
                : null;
        Integer clmAdmsnDt = rawClaim.getClmAdmsnDt() != null
                ? Integer.parseInt(rawClaim.getClmAdmsnDt().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE))
                : null;
        Integer nchBeneDschrgDt = rawClaim.getNchBeneDschrgDt() != null
                ? Integer.parseInt(rawClaim.getNchBeneDschrgDt().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE))
                : null;

        Integer segmentInt = null;
        if (rawClaim.getSegment() != null) {
            try {
                segmentInt = Integer.parseInt(rawClaim.getSegment());
            } catch (NumberFormatException ignored) {}
        }

        return PredictRequest.builder()
                .desynpufId(rawClaim.getDesynpufId())
                .clmId(String.valueOf(rawClaim.getRawClaimId()))
                .segment(segmentInt)
                .clmFromDt(clmFromDt)
                .clmThruDt(clmThruDt)
                .prvdrNum(rawClaim.getPrvdrNum())
                .clmPmtAmt(rawClaim.getClmPmtAmt())
                .nchPrmryPyrClmPdAmt(rawClaim.getNchPrmryPyrClmPdAmt())
                .atPhysnNpi(rawClaim.getAtPhysnNpi())
                .opPhysnNpi(rawClaim.getOpPhysnNpi())
                .otPhysnNpi(rawClaim.getOtPhysnNpi())
                .clmAdmsnDt(clmAdmsnDt)
                .admtngIcd9DgnsCd(rawClaim.getAdmtngIcd9DgnsCd())
                .clmPassThruPerDiemAmt(rawClaim.getClmPassThruPerDiemAmt())
                .nchBeneIpDdctblAmt(rawClaim.getNchBeneIpDdctblAmt())
                .nchBenePtaCoinsrncLbltyAm(rawClaim.getNchBenePtaCoinsrncLbltyAm())
                .nchBeneBloodDdctblLbltyAm(rawClaim.getNchBeneBloodDdctblLbltyAm())
                .clmUtlztnDayCnt(rawClaim.getClmUtlztnDayCnt())
                .nchBeneDschrgDt(nchBeneDschrgDt)
                .clmDrgCd(rawClaim.getClmDrgCd())
                .icd9DgnsCd1(icd9DgnsCd1)
                .icd9PrcdrCd1(icd9PrcdrCd1)
                .build();
    }

    // AnomalyResultDTO + RawClaim → ClaimPrediction entity (lưu DB)
    public ClaimPrediction toEntity(AnomalyResultDTO result, RawClaim rawClaim) {
        return ClaimPrediction.builder()
                .rawClaim(rawClaim)
                .modelName(result.getModelSelected())
                .modelVersion("1.0.0")
                .predictedLabel(result.getPrediction())
                .anomalyScore(result.getAnomalyScore())
                .riskPercentage(result.getRiskPercentage())
                .shouldAlert(result.getShouldAlert())
                .predictedAt(result.getTimestamp())
                .build();
    }

    public void enrichWithShap(ClaimPrediction prediction, ShapResultDTO shapResult) {
        prediction.setShapSummary(shapResult.getSummary());
        prediction.setShapConfidence(shapResult.getConfidence());
        prediction.setShapMethod(shapResult.getMethod());
        prediction.setShapFactors(toShapFactorEntities(shapResult, prediction));
    }

    // ShapResultDTO → List<ClaimShapFactor> entity (lưu DB)
    public List<ClaimShapFactor> toShapFactorEntities(ShapResultDTO shapResult,
                                                      ClaimPrediction prediction) {
        List<ShapFactorDTO> factors = shapResult.getTopFactors();
        List<ClaimShapFactor> entities = new ArrayList<>();

        for (ShapFactorDTO dto : factors) {
            entities.add(ClaimShapFactor.builder()
                    .claimPrediction(prediction)
                    .featureName(dto.getFeature())
                    .featureValue(String.valueOf(dto.getValue()))
                    .shapImpact(dto.getImpact())
                    .direction(dto.getDirection())                 // index + 1
                    .build());
        }
        return entities;
    }

    // ClaimPrediction entity → PredictionResponseDTO (trả về Frontend)
    public PredictionResponse toResponse(ClaimPrediction prediction) {
        List<ShapFactorDTO> factors = prediction.getShapFactors() == null
                ? Collections.emptyList()
                : prediction.getShapFactors().stream()
                .map(f -> {
                    ShapFactorDTO dto = new ShapFactorDTO();
                    dto.setFeature(group102.insurancefraud.util.FeatureLabelMap.getLabel(f.getFeatureName()));
                    dto.setImpact(f.getShapImpact());
                    dto.setDirection(f.getDirection());
                    dto.setValue(f.getFeatureValue());
                    return dto;
                })
                .collect(Collectors.toList());

        return PredictionResponse.builder()
                .predictionId(prediction.getPredictionId())
                .rawClaimId(prediction.getRawClaim().getRawClaimId())
                .predictedLabel(prediction.getPredictedLabel())
                .anomalyScore(prediction.getAnomalyScore())
                .riskPercentage(prediction.getRiskPercentage())
                .shouldAlert(prediction.getShouldAlert())
                .shapSummary(prediction.getShapSummary())
                .shapConfidence(prediction.getShapConfidence())
                .shapMethod(prediction.getShapMethod())
                .topFactors(factors)
                .predictedAt(prediction.getPredictedAt())
                .modelSelected(prediction.getModelName())
                .build();
    }
}
