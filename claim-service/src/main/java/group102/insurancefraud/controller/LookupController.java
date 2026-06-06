package group102.insurancefraud.controller;

import group102.insurancefraud.dto.response.ClaimResponse;
import group102.insurancefraud.entity.User;
import group102.insurancefraud.repository.UserRepository;
import group102.insurancefraud.service.RawClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/lookup")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'INVESTIGATOR', 'ADMIN')")
public class LookupController {

    private final RawClaimService rawClaimService;
    private final UserRepository userRepository;

    @GetMapping("/beneficiary")
    @PreAuthorize("hasAnyRole('STAFF', 'INVESTIGATOR', 'ADMIN')")
    public String lookupBeneficiary(@RequestParam(required = false) String query, Model model) {
        model.addAttribute("activeGroup", "lookup");
        model.addAttribute("activePage", "lookup-beneficiary");
        model.addAttribute("query", query);

        if (query != null && !query.trim().isEmpty()) {
            List<ClaimResponse> claims = rawClaimService.getClaimsByBeneficiaryId(query.trim());
            model.addAttribute("claims", claims);

            int totalClaims = claims.size();
            long rejectedClaims = claims.stream().filter(c -> "REJECTED".equals(c.getClaimStatus().name())).count();
            BigDecimal totalAmount = claims.stream()
                    .map(ClaimResponse::getClmPmtAmt)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("totalClaims", totalClaims);
            model.addAttribute("rejectedClaims", rejectedClaims);
            model.addAttribute("totalAmount", totalAmount);
        }
        model.addAttribute("breadcrumbParent", "Tra cứu");
        model.addAttribute("breadcrumbCurrent", "Theo Người thụ hưởng");

        return "lookup/beneficiary";
    }

    @GetMapping("/provider")
    @PreAuthorize("hasAnyRole('STAFF', 'INVESTIGATOR', 'ADMIN')")
    public String lookupProvider(@RequestParam(required = false) String query, Model model) {
        model.addAttribute("activeGroup", "lookup");
        model.addAttribute("activePage", "lookup-provider");
        model.addAttribute("query", query);

        if (query != null && !query.trim().isEmpty()) {
            List<ClaimResponse> claims = rawClaimService.getClaimsByProviderId(query.trim());
            model.addAttribute("claims", claims);

            int totalClaims = claims.size();
            long rejectedClaims = claims.stream().filter(c -> "REJECTED".equals(c.getClaimStatus().name())).count();
            BigDecimal totalAmount = claims.stream()
                    .map(ClaimResponse::getClmPmtAmt)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("totalClaims", totalClaims);
            model.addAttribute("rejectedClaims", rejectedClaims);
            model.addAttribute("totalAmount", totalAmount);
        }
        model.addAttribute("breadcrumbParent", "Tra cứu");
        model.addAttribute("breadcrumbCurrent", "Theo Nhà cung cấp");

        return "lookup/provider";
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public String lookupStaff(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userIdInput,
            Model model) {

        model.addAttribute("activeGroup", "lookup");
        model.addAttribute("activePage", "lookup-staff");

        // Load danh sách Staff + Investigator cho dropdown (sắp xếp theo tên)
        List<User> staffList = Stream.concat(
                userRepository.findByRoleIgnoreCase("STAFF").stream(),
                userRepository.findByRoleIgnoreCase("INVESTIGATOR").stream()
        ).sorted(Comparator.comparing(User::getFullName)).collect(Collectors.toList());
        model.addAttribute("staffList", staffList);

        // Ưu tiên userIdInput (nhập tay) nếu có, sẽ ghi đè dropdown
        Long resolvedId = userId;
        if (userIdInput != null && !userIdInput.trim().isEmpty()) {
            try {
                resolvedId = Long.parseLong(userIdInput.trim());
            } catch (NumberFormatException ignored) {
                model.addAttribute("idError", "ID nhân viên không hợp lệ, vui lòng nhập số nguyên.");
            }
        }

        model.addAttribute("selectedUserId", resolvedId);

        if (resolvedId != null) {
            userRepository.findById(resolvedId).ifPresent(u -> model.addAttribute("selectedUser", u));

            List<ClaimResponse> claims = rawClaimService.getClaimsByUser(resolvedId);
            model.addAttribute("claims", claims);

            int totalClaims = claims.size();
            long rejectedClaims = claims.stream().filter(c -> "REJECTED".equals(c.getClaimStatus().name())).count();
            long approvedClaims = claims.stream().filter(c -> "APPROVED".equals(c.getClaimStatus().name())).count();
            BigDecimal totalAmount = claims.stream()
                    .map(ClaimResponse::getClmPmtAmt)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("totalClaims", totalClaims);
            model.addAttribute("rejectedClaims", rejectedClaims);
            model.addAttribute("approvedClaims", approvedClaims);
            model.addAttribute("totalAmount", totalAmount);
        }
        model.addAttribute("breadcrumbParent", "Tra cứu");
        model.addAttribute("breadcrumbCurrent", "Theo Nhân viên");

        return "lookup/staff";
    }
}
