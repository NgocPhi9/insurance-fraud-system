package group102.insurancefraud.controller;

import group102.insurancefraud.dto.response.ClaimResponse;
import group102.insurancefraud.dto.response.UserResponse;
import group102.insurancefraud.security.CustomUserDetails;
import group102.insurancefraud.service.RawClaimService;
import group102.insurancefraud.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@Controller
@RequestMapping("/claims")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class ClaimViewController extends BaseController {

    private final RawClaimService rawClaimService;
    private final UserService userService;

    // Danh sách claims (có thể lọc theo status)
    @GetMapping
    public String listClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean overdue,
            @RequestParam(required = false, defaultValue = "false") boolean unassigned,
            @RequestParam(required = false, defaultValue = "false") boolean hasAlert,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Page<ClaimResponse> claimsPage;
        String filterLabel = null;

        if (hasAlert && "INVESTIGATOR".equals(userDetails.getUser().getRole())) {
            claimsPage = rawClaimService.getAlertClaimsByInvestigator(userDetails.getUserId(), page);
            filterLabel = "Alerts được giao";
        } else if (overdue && "INVESTIGATOR".equals(userDetails.getUser().getRole())) {
            claimsPage = rawClaimService.getOverdueClaims(userDetails.getUserId(), 7, page);
            filterLabel = "Case quá hạn (> 7 ngày)";
        } else if (unassigned && "STAFF".equals(userDetails.getUser().getRole())) {
            claimsPage = rawClaimService.getUnassignedByStaff(userDetails.getUserId(), page);
            filterLabel = "Hồ sơ chưa có điều tra viên";
        } else if (unassigned && "ADMIN".equals(userDetails.getUser().getRole())) {
            claimsPage = rawClaimService.getUnassignedPending(page);
            filterLabel = "Claims PENDING chưa được phân công";
        } else if (status != null && !status.isBlank()) {
            try {
                group102.insurancefraud.enums.ClaimStatus claimStatus =
                        group102.insurancefraud.enums.ClaimStatus.valueOf(status.toUpperCase());
                if ("INVESTIGATOR".equals(userDetails.getUser().getRole())) {
                    claimsPage = rawClaimService.getClaimsByInvestigatorAndStatus(userDetails.getUserId(), claimStatus, page);
                } else if ("STAFF".equals(userDetails.getUser().getRole())) {
                    claimsPage = rawClaimService.getClaimsByHandlerAndStatus(userDetails.getUserId(), claimStatus, page);
                } else {
                    claimsPage = rawClaimService.getClaimsByStatus(claimStatus, page);
                }
                filterLabel = "Trạng thái: " + status.toUpperCase();
            } catch (IllegalArgumentException e) {
                claimsPage = rawClaimService.getAllClaims(page);
            }
        } else if ("STAFF".equals(userDetails.getUser().getRole())) {
            claimsPage = rawClaimService.getClaimsByHandler(userDetails.getUserId(), page);
        } else {
            claimsPage = rawClaimService.getAllClaims(page);
        }

        model.addAttribute("claimsPage", claimsPage);
        model.addAttribute("claims", claimsPage.getContent());
        model.addAttribute("pageNumbers", buildPageNumbers(claimsPage.getNumber(), claimsPage.getTotalPages()));
        model.addAttribute("filterLabel", filterLabel);
        model.addAttribute("activePage", "claims-list");
        model.addAttribute("activeGroup", "claims");
        model.addAttribute("breadcrumbParent", "Claims");
        model.addAttribute("breadcrumbCurrent", filterLabel != null ? filterLabel : "Danh sách");
        return "claims/list";
    }

    // Danh sách claims của investigator
    @GetMapping("/my-claims")
    @PreAuthorize("hasRole('INVESTIGATOR')")
    public String myClaims(
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Page<ClaimResponse> claimsPage = rawClaimService.getClaimsByInvestigator(userDetails.getUserId(), page);

        model.addAttribute("claimsPage", claimsPage);
        model.addAttribute("claims", claimsPage.getContent());
        model.addAttribute("pageNumbers", buildPageNumbers(claimsPage.getNumber(), claimsPage.getTotalPages()));
        model.addAttribute("activePage", "claims-my");
        model.addAttribute("activeGroup", "claims");
        model.addAttribute("breadcrumbParent", "Claims");
        model.addAttribute("breadcrumbCurrent", "Claims của tôi");
        return "claims/list";
    }

    /**
     * Xây dựng danh sách số trang với dấu ellipsis.
     * null = hiển thị “…”, Integer = số trang (0-indexed).
     * Ví dụ: current=9, total=50 → [0, 1, null, 7, 8, 9, 10, 11, null, 49]
     */
    private List<Integer> buildPageNumbers(int current, int total) {
        if (total <= 1) return List.of();

        TreeSet<Integer> set = new TreeSet<>();
        // Luôn hiển 2 trang đầu và 2 trang cuối
        set.add(0);
        if (total > 1) set.add(1);
        set.add(total - 1);
        if (total > 1) set.add(total - 2);
        // Window 2 trang xung quanh trang hiện tại
        for (int i = Math.max(0, current - 2); i <= Math.min(total - 1, current + 2); i++) {
            set.add(i);
        }

        List<Integer> result = new ArrayList<>();
        int prev = -1;
        for (int p : set) {
            if (p - prev > 1) result.add(null);   // khoảng cách → dấu ...
            result.add(p);
            prev = p;
        }
        return result;
    }

    // Form tạo claim
    @GetMapping("/create")
    public String createClaimForm(Model model) {
        model.addAttribute("activePage", "claims-create");
        model.addAttribute("activeGroup", "claims");
        model.addAttribute("breadcrumbParent", "Claims");
        model.addAttribute("breadcrumbCurrent", "Tạo mới");
        return "claims/create";
    }

    // Claims của một user cụ thể (chỉ ADMIN)
    @GetMapping("/by-user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String claimsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        UserResponse targetUser = userService.getUserById(userId);
        List<ClaimResponse> allClaims = rawClaimService.getClaimsByUser(userId);

        // Phân trang thủ công từ list đã merge
        int pageSize = 20;
        int totalItems = allClaims.size();
        int start = Math.min(page * pageSize, totalItems);
        int end   = Math.min(start + pageSize, totalItems);
        List<ClaimResponse> pageContent = allClaims.subList(start, end);

        Page<ClaimResponse> claimsPage = new PageImpl<>(
                pageContent,
                PageRequest.of(page, pageSize),
                totalItems
        );

        model.addAttribute("targetUser", targetUser);
        model.addAttribute("claimsPage", claimsPage);
        model.addAttribute("claims", claimsPage.getContent());
        model.addAttribute("pageNumbers", buildPageNumbers(claimsPage.getNumber(), claimsPage.getTotalPages()));
        model.addAttribute("activeGroup", "claims");
        model.addAttribute("activePage", "claims-list");
        model.addAttribute("breadcrumbParent", "Claims");
        model.addAttribute("breadcrumbCurrent", "Claims của " + targetUser.getFullName());
        return "claims/by-user";
    }

    // Detail claim
    @GetMapping("/{id}")
    public String claimDetail(@PathVariable Long id, Model model) {
        ClaimResponse claim = rawClaimService.getClaimById(id);
        model.addAttribute("claim", claim);
        model.addAttribute("activeGroup", "claims");
        model.addAttribute("breadcrumbParent", "Claims");
        model.addAttribute("breadcrumbCurrent", "Chi tiết #" + id);
        return "claims/detail";
    }

}