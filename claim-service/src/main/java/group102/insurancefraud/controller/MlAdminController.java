package group102.insurancefraud.controller;

import group102.insurancefraud.dto.response.RetrainResponseDTO;
import group102.insurancefraud.security.CustomUserDetails;
import group102.insurancefraud.service.MlClientService;
import group102.insurancefraud.service.RetrainStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/ml")
@RequiredArgsConstructor
@Slf4j
public class MlAdminController extends BaseController {

    private final MlClientService mlClientService;
    private final RetrainStateService retrainStateService;
    private final PasswordEncoder passwordEncoder;

    @Value("${ml.images.base-url}")
    private String mlImagesBaseUrl;

    @GetMapping("/retrain")
    @PreAuthorize("hasRole('ADMIN')")
    public String getRetrainPage(Model model) {
        boolean inProgress = retrainStateService.isRetrainingInProgress();
        try {
            RetrainResponseDTO currentInfo = mlClientService.getRetrainInfo();
            model.addAttribute("currentInfo", currentInfo);
        } catch (Exception e) {
            log.warn("Could not fetch current model info", e);
        }

        model.addAttribute("activeGroup", "ml");
        model.addAttribute("activePage", "ml-retrain");
        model.addAttribute("breadcrumbParent", "Phân tích Gian lận");
        model.addAttribute("breadcrumbCurrent", "Retrain Model");
        model.addAttribute("mlImagesBaseUrl", mlImagesBaseUrl);
        model.addAttribute("retrainInProgress", inProgress);
        return "admin/ml-retrain";
    }

    @GetMapping("/retrain/status")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Map<String, Boolean> getRetrainStatus() {
        return Map.of("inProgress", retrainStateService.isRetrainingInProgress());
    }

    @PostMapping("/retrain/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public String executeRetrain(
            @RequestParam("adminPassword") String adminPassword,
            @RequestParam(value = "confirmRetrain", defaultValue = "false") boolean confirmRetrain,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            if (!confirmRetrain) {
                redirectAttributes.addFlashAttribute("retrainModalError", "Bạn cần xác nhận trước khi retrain model.");
                redirectAttributes.addFlashAttribute("showRetrainModal", true);
                return "redirect:/admin/ml/retrain";
            }

            if (adminPassword == null || adminPassword.isBlank()
                    || !passwordEncoder.matches(adminPassword, userDetails.getPassword())) {
                redirectAttributes.addFlashAttribute("retrainModalError", "Mật khẩu xác nhận không chính xác.");
                redirectAttributes.addFlashAttribute("showRetrainModal", true);
                return "redirect:/admin/ml/retrain";
            }

            if (retrainStateService.isRetrainingInProgress()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Quá trình huấn luyện lại đang diễn ra.");
                return "redirect:/admin/ml/retrain";
            }

            RetrainResponseDTO currentInfo = mlClientService.getRetrainInfo();
            String currentVersion = null;
            if (currentInfo != null && currentInfo.getRetrains() != null && !currentInfo.getRetrains().isEmpty()) {
                currentVersion = currentInfo.getRetrains().get(currentInfo.getRetrains().size() - 1).getVersion();
            }

            mlClientService.retrain();
            retrainStateService.startRetrain(currentVersion);

            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu retrain model. Quá trình này có thể mất thời gian.");
            return "redirect:/admin/ml/retrain";
        } catch (Exception e) {
            log.error("Retrain request failed", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể gửi yêu cầu retrain: " + e.getMessage());
            return "redirect:/admin/ml/retrain";
        }
    }
}
