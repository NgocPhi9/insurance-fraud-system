package group102.insurancefraud.controller;

import group102.insurancefraud.dto.response.UserResponse;
import group102.insurancefraud.security.CustomUserDetails;
import group102.insurancefraud.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController extends BaseController {

    private final UserService userService;

    @GetMapping
    public String viewProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        UserResponse user = userService.getUserById(userDetails.getUserId());

        model.addAttribute("profile", user);
        
        if (!model.containsAttribute("profileForm")) {
            group102.insurancefraud.dto.request.UpdateProfileRequest profileForm = new group102.insurancefraud.dto.request.UpdateProfileRequest();
            profileForm.setFullName(user.getFullName());
            profileForm.setPhoneNumber(user.getPhoneNumber());
            model.addAttribute("profileForm", profileForm);
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new group102.insurancefraud.dto.request.ChangePasswordRequest());
        }

        model.addAttribute("activePage", "profile");
        model.addAttribute("breadcrumbCurrent", "Hồ sơ của tôi");
        return "profile/index";
    }

    @PostMapping("/update")
    public String updateProfile(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.ModelAttribute("profileForm") group102.insurancefraud.dto.request.UpdateProfileRequest request,
            org.springframework.validation.BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            UserResponse user = userService.getUserById(userDetails.getUserId());
            model.addAttribute("profile", user);
            if (!model.containsAttribute("passwordForm")) {
                model.addAttribute("passwordForm", new group102.insurancefraud.dto.request.ChangePasswordRequest());
            }
            model.addAttribute("activePage", "profile");
            model.addAttribute("breadcrumbCurrent", "Hồ sơ của tôi");
            model.addAttribute("profileErrorMsg", "Thông tin nhập vào không hợp lệ");
            return "profile/index";
        }

        try {
            userService.updateProfile(userDetails.getUserId(), request);
            
            redirectAttributes.addFlashAttribute("profileSuccessMsg", "Cập nhật thông tin thành công!");
            return "redirect:/profile";
        } catch (Exception e) {
            UserResponse user = userService.getUserById(userDetails.getUserId());
            model.addAttribute("profile", user);
            if (!model.containsAttribute("passwordForm")) {
                model.addAttribute("passwordForm", new group102.insurancefraud.dto.request.ChangePasswordRequest());
            }
            model.addAttribute("activePage", "profile");
            model.addAttribute("breadcrumbCurrent", "Hồ sơ của tôi");
            model.addAttribute("profileErrorMsg", e.getMessage());
            return "profile/index";
        }
    }

    @PostMapping("/change-password")
    public String changePassword(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.ModelAttribute("passwordForm") group102.insurancefraud.dto.request.ChangePasswordRequest request,
            org.springframework.validation.BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            UserResponse user = userService.getUserById(userDetails.getUserId());
            model.addAttribute("profile", user);
            if (!model.containsAttribute("profileForm")) {
                group102.insurancefraud.dto.request.UpdateProfileRequest profileForm = new group102.insurancefraud.dto.request.UpdateProfileRequest();
                profileForm.setFullName(user.getFullName());
                profileForm.setPhoneNumber(user.getPhoneNumber());
                model.addAttribute("profileForm", profileForm);
            }
            model.addAttribute("activePage", "profile");
            model.addAttribute("breadcrumbCurrent", "Hồ sơ của tôi");
            model.addAttribute("passwordErrorMsg", "Vui lòng điền đầy đủ thông tin mật khẩu");
            return "profile/index";
        }

        try {
            userService.changePassword(userDetails.getUserId(), request);
            redirectAttributes.addFlashAttribute("passwordSuccessMsg", "Đổi mật khẩu thành công!");
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            UserResponse user = userService.getUserById(userDetails.getUserId());
            model.addAttribute("profile", user);
            if (!model.containsAttribute("profileForm")) {
                group102.insurancefraud.dto.request.UpdateProfileRequest profileForm = new group102.insurancefraud.dto.request.UpdateProfileRequest();
                profileForm.setFullName(user.getFullName());
                profileForm.setPhoneNumber(user.getPhoneNumber());
                model.addAttribute("profileForm", profileForm);
            }
            model.addAttribute("activePage", "profile");
            model.addAttribute("breadcrumbCurrent", "Hồ sơ của tôi");
            model.addAttribute("passwordErrorMsg", e.getMessage());
            return "profile/index";
        }
    }
}
