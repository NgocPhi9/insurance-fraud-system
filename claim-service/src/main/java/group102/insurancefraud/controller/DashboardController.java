package group102.insurancefraud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// controller/DashboardController.java
@Controller
public class DashboardController extends BaseController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("breadcrumbCurrent", "Dashboard");
        model.addAttribute("activePage", "dashboard");
        return "dashboard/index";
    }
}
