package group102.insurancefraud.service;

import group102.insurancefraud.dto.request.InvestigationActionRequest;
import group102.insurancefraud.dto.response.ClaimResponse;
import group102.insurancefraud.dto.response.InvestigationResponse;
import group102.insurancefraud.entity.ClaimInvestigation;
import group102.insurancefraud.entity.RawClaim;
import group102.insurancefraud.entity.User;
import group102.insurancefraud.enums.ClaimStatus;
import group102.insurancefraud.exception.ResourceNotFoundException;
import group102.insurancefraud.mapper.ClaimMapper;
import group102.insurancefraud.repository.ClaimInvestigationRepository;
import group102.insurancefraud.repository.RawClaimRepository;
import group102.insurancefraud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvestigationService {

    private final RawClaimRepository            rawClaimRepository;
    private final UserRepository                userRepository;
    private final ClaimInvestigationRepository  investigationRepository;
    private final ClaimMapper                   claimMapper;

    // ── Thực hiện action: APPROVE / REJECT / NOTE ─────
    @Transactional
    public ClaimResponse performAction(Long claimId,
                                       InvestigationActionRequest request,
                                       String investigatorEmail) {
        RawClaim claim = rawClaimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + claimId));

        User investigator = userRepository.findByEmail(investigatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Chỉ investigator được assign mới thao tác được
        if (claim.getInvestigator() == null ||
                !claim.getInvestigator().getUserId().equals(investigator.getUserId())) {
            throw new RuntimeException("Bạn không được phân công điều tra claim này");
        }

        // Validate note bắt buộc khi REJECT
        if ("REJECT".equals(request.getAction()) &&
                (request.getNote() == null || request.getNote().isBlank())) {
            throw new RuntimeException("Vui lòng nhập lý do từ chối");
        }

        // Cập nhật status claim
        switch (request.getAction()) {
            case "APPROVE" -> {
                claim.setClaimStatus(ClaimStatus.APPROVED);
                claim.setResolvedAt(LocalDateTime.now());
            }
            case "REJECT"  -> {
                claim.setClaimStatus(ClaimStatus.REJECTED);
                claim.setResolvedAt(LocalDateTime.now());
            }
            case "NOTE"    -> { /* giữ nguyên status */ }
            default -> throw new RuntimeException("Action không hợp lệ: " + request.getAction());
        }

        // Lưu lịch sử
        ClaimInvestigation history = ClaimInvestigation.builder()
                .rawClaim(claim)
                .investigator(investigator)
                .action(request.getAction())
                .note(request.getNote())
                .build();
        investigationRepository.save(history);

        rawClaimRepository.save(claim);
        log.info("Claim [{}] action [{}] by [{}]",
                claimId, request.getAction(), investigatorEmail);

        return claimMapper.toResponse(claim);
    }

    // ── Lấy lịch sử điều tra ─────────────────────────
    @Transactional(readOnly = true)
    public List<InvestigationResponse> getHistory(Long claimId) {
        return investigationRepository
                .findByRawClaim_RawClaimIdOrderByCreatedAtDesc(claimId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private InvestigationResponse toResponse(ClaimInvestigation inv) {
        return InvestigationResponse.builder()
                .investigationId(inv.getInvestigationId())
                .rawClaimId(inv.getRawClaim().getRawClaimId())
                .investigatorName(inv.getInvestigator().getFullName())
                .action(inv.getAction())
                .note(inv.getNote())
                .createdAt(inv.getCreatedAt())
                .build();
    }
}