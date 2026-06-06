package group102.insurancefraud.controller;

import group102.insurancefraud.service.FraudIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/fraud-intelligence")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('INVESTIGATOR', 'ADMIN')")
public class FraudIntelligenceController {

    private final FraudIntelligenceService fraudIntelligenceService;

    @GetMapping
    public String viewFraudIntelligence(Model model) {
        model.addAttribute("activeGroup", "predictions");
        model.addAttribute("activePage", "fraud-suspects");
        model.addAttribute("breadcrumbParent", "Phân tích Gian lận");
        model.addAttribute("breadcrumbCurrent", "Đối tượng nghi ngờ");

        model.addAttribute("topBeneficiaries", fraudIntelligenceService.getTopFraudulentBeneficiaries());
        model.addAttribute("topProviders", fraudIntelligenceService.getTopFraudulentProviders());

        Map<String, Object> stats = fraudIntelligenceService.getSummaryStats();
        model.addAllAttributes(stats);

        return "fraud-intelligence/index";
    }
}
