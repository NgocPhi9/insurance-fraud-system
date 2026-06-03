package group102.insurancefraud.controller;

import group102.insurancefraud.dto.request.InvestigationActionRequest;
import group102.insurancefraud.dto.response.ApiResponse;
import group102.insurancefraud.dto.response.ClaimResponse;
import group102.insurancefraud.dto.response.InvestigationResponse;
import group102.insurancefraud.security.CustomUserDetails;
import group102.insurancefraud.service.InvestigationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/investigation")
@RequiredArgsConstructor
@Slf4j
public class InvestigationController {

    private final InvestigationService investigationService;

    // APPROVE / REJECT / NOTE
    @PostMapping("/claims/{claimId}/action")
    @PreAuthorize("hasRole('INVESTIGATOR')")
    public ResponseEntity<ApiResponse<ClaimResponse>> performAction(
            @PathVariable Long claimId,
            @Valid @RequestBody InvestigationActionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ClaimResponse result = investigationService.performAction(
                claimId, request, userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.<ClaimResponse>builder()
                .success(true)
                .message("Thực hiện thành công")
                .data(result)
                .build());
    }

    // Lịch sử điều tra
    @GetMapping("/claims/{claimId}/history")
    @PreAuthorize("hasAnyRole('INVESTIGATOR','ADMIN')")
    public ResponseEntity<ApiResponse<List<InvestigationResponse>>> getHistory(
            @PathVariable Long claimId) {

        List<InvestigationResponse> history = investigationService.getHistory(claimId);
        return ResponseEntity.ok(ApiResponse.<List<InvestigationResponse>>builder()
                .success(true)
                .message("OK")
                .data(history)
                .build());
    }
}