package group102.insurancefraud.controller;

import group102.insurancefraud.dto.response.DashboardMetricsDto;
import group102.insurancefraud.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class DashboardController extends BaseController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal group102.insurancefraud.security.CustomUserDetails userDetails,
                            @RequestParam(defaultValue = "30") int range,
                            Model model) {
        model.addAttribute("breadcrumbCurrent", "Dashboard");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("selectedRange", range);
        
        DashboardMetricsDto metrics = dashboardService.getMetricsForUser(userDetails.getUser(), range);
        model.addAttribute("metrics", metrics);
        model.addAttribute("role", userDetails.getRole());

        return "dashboard/index";
    }
}
