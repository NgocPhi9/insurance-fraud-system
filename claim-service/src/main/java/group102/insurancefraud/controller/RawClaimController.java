package group102.insurancefraud.controller;

import group102.insurancefraud.dto.response.ApiResponse;
import group102.insurancefraud.dto.response.ClaimResponse;
import group102.insurancefraud.dto.request.CreateClaimRequest;
import group102.insurancefraud.security.CustomUserDetails;
import group102.insurancefraud.service.RawClaimService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class RawClaimController {

    private final RawClaimService rawClaimService;

    public RawClaimController(RawClaimService rawClaimService) {
        this.rawClaimService = rawClaimService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<ClaimResponse>> createClaim(
            @Valid @RequestBody CreateClaimRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ClaimResponse claim = rawClaimService.createClaim(request, userDetails.getUsername());
        return new ResponseEntity<>(
                ApiResponse.<ClaimResponse>builder()
                        .success(true)
                        .message("Claim created successfully")
                        .data(claim)
                        .build(),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClaimResponse>> getClaimById(
            @PathVariable Long id
    ) {

        ClaimResponse claim = rawClaimService.getClaimById(id);

        ApiResponse<ClaimResponse> response =
                ApiResponse.<ClaimResponse>builder()
                        .success(true)
                        .message("Claim retrieved successfully")
                        .data(claim)
                        .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClaimResponse>> updateClaim(
            @PathVariable Long id,
            @RequestBody CreateClaimRequest request
    ) {

        ClaimResponse updatedClaim =
                rawClaimService.updateClaim(id, request);

        ApiResponse<ClaimResponse> response =
                ApiResponse.<ClaimResponse>builder()
                        .success(true)
                        .message("Claim updated successfully")
                        .data(updatedClaim)
                        .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteClaim(
            @PathVariable Long id
    ) {

        rawClaimService.deleteClaim(id);

        ApiResponse<Object> response =
                ApiResponse.builder()
                        .success(true)
                        .message("Claim deleted successfully")
                        .data(null)
                        .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('INVESTIGATOR')")
    public ResponseEntity<ApiResponse<ClaimResponse>> assignInvestigator(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ClaimResponse claim = rawClaimService.assignInvestigator(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.<ClaimResponse>builder()
                .success(true)
                .message("Nhận claim thành công")
                .data(claim)
                .build());
    }
}
