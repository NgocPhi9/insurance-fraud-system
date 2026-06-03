package group102.insurancefraud.controller;

import group102.insurancefraud.dto.request.CreateClaimRequest;
import group102.insurancefraud.dto.response.ClaimResponse;
import group102.insurancefraud.service.RawClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;

@Controller
@RequestMapping("/claims")
@RequiredArgsConstructor
public class ClaimViewController extends BaseController {

    private final RawClaimService rawClaimService;

    // Danh sách claims
    @GetMapping
    public String listClaims(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<ClaimResponse> claimsPage = rawClaimService.getAllClaims(page);
        model.addAttribute("claimsPage", claimsPage);
        model.addAttribute("claims", claimsPage.getContent());
        model.addAttribute("pageNumbers", buildPageNumbers(claimsPage.getNumber(), claimsPage.getTotalPages()));
        model.addAttribute("activePage", "claims-list");
        model.addAttribute("activeGroup", "claims");
        model.addAttribute("breadcrumbParent", "Claims");
        model.addAttribute("breadcrumbCurrent", "Danh sách");
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