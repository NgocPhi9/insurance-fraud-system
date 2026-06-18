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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

        // ── VALIDATION NGHIỆP VỤ ────────────────────────────────────────────

        // Quy tắc 1: Ngày xuất viện phải >= ngày nhập viện
        if (request.getNchBeneDschrgDt() != null && request.getClmAdmsnDt() != null
                && request.getNchBeneDschrgDt().isBefore(request.getClmAdmsnDt())) {
            throw new IllegalArgumentException("Ngày xuất viện không được trước ngày nhập viện");
        }

        // Quy tắc 2: Ngày bắt đầu claim (nếu cung cấp) không được trước ngày nhập viện
        if (request.getClmFromDt() != null && request.getClmAdmsnDt() != null
                && request.getClmFromDt().isBefore(request.getClmAdmsnDt())) {
            throw new IllegalArgumentException("Ngày bắt đầu claim không được trước ngày nhập viện");
        }

        // Quy tắc 3: Ngày kết thúc claim (nếu cung cấp) không được trước ngày xuất viện
        if (request.getClmThruDt() != null && request.getNchBeneDschrgDt() != null
                && request.getClmThruDt().isBefore(request.getNchBeneDschrgDt())) {
            throw new IllegalArgumentException("Ngày kết thúc claim không được trước ngày xuất viện");
        }

        // Quy tắc 4: Bác sĩ phẫu thuật và thủ thuật ICD-9 phải đi kèm nhau
        boolean hasProcedures = request.getProcedures() != null && !request.getProcedures().isEmpty();
        boolean hasOpPhysnNpi = request.getOpPhysnNpi() != null && !request.getOpPhysnNpi().isBlank();
        
        if (hasProcedures && !hasOpPhysnNpi) {
            throw new IllegalArgumentException("Mã NPI bác sĩ phẫu thuật bắt buộc khi có thủ thuật ICD-9");
        }
        if (hasOpPhysnNpi && !hasProcedures) {
            throw new IllegalArgumentException("Phải nhập ít nhất một thủ thuật ICD-9 khi đã có bác sĩ phẫu thuật");
        }

        // Quy tắc 5: Số ngày điều trị không được vượt quá thời gian nằm viện thực tế
        if (request.getClmUtlztnDayCnt() != null
                && request.getClmAdmsnDt() != null
                && request.getNchBeneDschrgDt() != null) {
            long maxDays = ChronoUnit.DAYS.between(
                    request.getClmAdmsnDt(), request.getNchBeneDschrgDt());
            if (request.getClmUtlztnDayCnt() > maxDays) {
                throw new IllegalArgumentException(
                    "Số ngày điều trị (đang nhập: " + request.getClmUtlztnDayCnt()
                    + ") không được lớn hơn số ngày nằm viện thực tế (" + maxDays + " ngày)");
            }
        }

        // ── MAP VÀ TỰ TÍNH CÁC TRƯỜNG ────────────────────────────────────────

        RawClaim claim = claimMapper.toEntity(request);

        // Đảm bảo claimStatus không bị null do @Builder bỏ qua giá trị default của field
        if (claim.getClaimStatus() == null) {
            claim.setClaimStatus(group102.insurancefraud.enums.ClaimStatus.PENDING);
        }

        // Auto-set SEGMENT = "1" nếu không cung cấp
        if (claim.getSegment() == null || claim.getSegment().isBlank()) {
            claim.setSegment("1");
        }

        // Auto-calc CLM_FROM_DT = CLM_ADMSN_DT nếu không cung cấp
        if (claim.getClmFromDt() == null && claim.getClmAdmsnDt() != null) {
            claim.setClmFromDt(claim.getClmAdmsnDt());
        }

        // Auto-calc CLM_THRU_DT = NCH_BENE_DSCHRG_DT nếu không cung cấp
        if (claim.getClmThruDt() == null && claim.getNchBeneDschrgDt() != null) {
            claim.setClmThruDt(claim.getNchBeneDschrgDt());
        }

        // Auto-calc CLM_UTLZTN_DAY_CNT từ khoảng cách nhập-xuất viện nếu không cung cấp
        if (claim.getClmUtlztnDayCnt() == null
                && claim.getClmAdmsnDt() != null
                && claim.getNchBeneDschrgDt() != null) {
            long days = ChronoUnit.DAYS.between(claim.getClmAdmsnDt(), claim.getNchBeneDschrgDt());
            claim.setClmUtlztnDayCnt((int) Math.max(days, 1));
        }

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

    public Page<ClaimResponse> getClaimsByHandler(Long handlerId, int page) {
        Pageable pageable = PageRequest.of(
                page,
                appConfig.getPageSize(),
                Sort.by(Sort.Direction.DESC, "rawClaimId")
        );
        return rawClaimRepository.findByClaimHandler_UserId(handlerId, pageable)
                .map(claimMapper::toResponse);
    }

    public Page<ClaimResponse> getClaimsByInvestigator(Long investigatorId, int page) {
        Pageable pageable = PageRequest.of(
                page,
                appConfig.getPageSize(),
                Sort.by(Sort.Direction.DESC, "rawClaimId")
        );
        return rawClaimRepository.findByInvestigator_UserId(investigatorId, pageable)
                .map(claimMapper::toResponse);
    }

    public ClaimResponse getClaimById(Long id) {

        RawClaim claim = rawClaimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));

        return claimMapper.toResponse(claim);
    }

    public List<ClaimResponse> getClaimsByUser(Long userId) {
        // Gộp claims mà user là handler (STAFF) hoặc investigator (INVESTIGATOR)
        // Dùng LinkedHashMap để dedup theo rawClaimId, giữ thứ tự insert
        java.util.Map<Long, ClaimResponse> merged = new java.util.LinkedHashMap<>();

        rawClaimRepository.findByClaimHandler_UserId(userId)
                .stream()
                .map(claimMapper::toResponse)
                .forEach(c -> merged.put(c.getRawClaimId(), c));

        rawClaimRepository.findByInvestigator_UserId(userId)
                .stream()
                .map(claimMapper::toResponse)
                .forEach(c -> merged.put(c.getRawClaimId(), c));

        return new java.util.ArrayList<>(merged.values());
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

    public List<ClaimResponse> getClaimsByBeneficiaryId(String beneficiaryId) {
        return rawClaimRepository.findByDesynpufIdContainingIgnoreCase(beneficiaryId)
                .stream()
                .map(claimMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<ClaimResponse> getClaimsByProviderId(String providerId) {
        return rawClaimRepository.findByPrvdrNumContainingIgnoreCase(providerId)
                .stream()
                .map(claimMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }
}