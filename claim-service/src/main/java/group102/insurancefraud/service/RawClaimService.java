package group102.insurancefraud.service;

import group102.insurancefraud.config.AppConfig;
import group102.insurancefraud.dto.response.ClaimResponse;
import group102.insurancefraud.dto.request.CreateClaimRequest;
import group102.insurancefraud.entity.RawClaim;
import group102.insurancefraud.entity.User;
import group102.insurancefraud.enums.ClaimStatus;
import group102.insurancefraud.exception.ClaimAlreadyAssignedException;
import group102.insurancefraud.exception.ResourceNotFoundException;
import group102.insurancefraud.mapper.ClaimMapper;
import group102.insurancefraud.repository.RawClaimRepository;
import group102.insurancefraud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RawClaimService {

    private final RawClaimRepository rawClaimRepository;
    private final ClaimMapper claimMapper;
    private final PredictionService predictionService;
    private final UserRepository userRepository;
    private final AppConfig appConfig;

    public ClaimResponse createClaim(CreateClaimRequest request, String creatorEmail) {

        RawClaim claim = claimMapper.toEntity(request);

        userRepository.findByEmail(creatorEmail)
                .ifPresent(claim::setClaimHandler);

        RawClaim savedClaim = rawClaimRepository.save(claim);

        predictionService.predictAsync(savedClaim.getRawClaimId());

        return claimMapper.toResponse(savedClaim);
    }

    public Page<ClaimResponse> getAllClaims(int page) {
        Pageable pageable = PageRequest.of(
                page,
                appConfig.getPageSize(),
                Sort.by(Sort.Direction.DESC, "rawClaimId")
        );
        return rawClaimRepository.findAll(pageable)
                .map(claimMapper::toResponse);
    }

    public ClaimResponse getClaimById(Long id) {

        RawClaim claim = rawClaimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));

        return claimMapper.toResponse(claim);
    }

    public ClaimResponse updateClaim(Long id, CreateClaimRequest request) {

        RawClaim existingClaim = rawClaimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));

        existingClaim.setSegment(request.getSegment());
        existingClaim.setClmPmtAmt(request.getClmPmtAmt());
        if (request.getClaimStatus() != null) {
            existingClaim.setClaimStatus(request.getClaimStatus());
        }

        RawClaim updatedClaim = rawClaimRepository.save(existingClaim);

        return claimMapper.toResponse(updatedClaim);
    }

    public void deleteClaim(Long id) {

        RawClaim claim = rawClaimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));

        rawClaimRepository.delete(claim);
    }

    @Transactional
    public ClaimResponse assignInvestigator(Long claimId, String investigatorEmail) {
        RawClaim claim = rawClaimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + claimId));

        // Chỉ nhận claim ở trạng thái FLAGGED
        if (claim.getClaimStatus() != ClaimStatus.FLAGGED) {
            throw new RuntimeException(
                    "Chỉ có thể nhận claim ở trạng thái FLAGGED. " +
                            "Trạng thái hiện tại: " + claim.getClaimStatus()
            );
        }

        if (claim.getInvestigator() != null) {
            throw new ClaimAlreadyAssignedException(
                    "Claim này đã được nhận bởi: " + claim.getInvestigator().getFullName()
            );
        }

        User investigator = userRepository.findByEmail(investigatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long activeCount = rawClaimRepository
                .countActiveClaimsByInvestigator(investigator.getUserId());
        if (activeCount >= appConfig.getMaxClaimsPerInvestigator()) {
            throw new RuntimeException(
                    "Bạn đang xử lý " + activeCount + " claims. " +
                            "Tối đa " + appConfig.getMaxClaimsPerInvestigator() + " claims cùng lúc."
            );
        }

        claim.setInvestigator(investigator);
        claim.setClaimStatus(ClaimStatus.UNDER_REVIEW);  // FLAGGED → UNDER_REVIEW
        return claimMapper.toResponse(rawClaimRepository.save(claim));
    }
}