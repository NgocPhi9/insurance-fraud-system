package group102.insurancefraud.controller;

import group102.insurancefraud.dto.request.CreateUserRequest;
import group102.insurancefraud.dto.request.UpdateUserRequest;
import group102.insurancefraud.dto.response.UserResponse;
import group102.insurancefraud.enums.UserRole;
import group102.insurancefraud.security.CustomUserDetails;
import group102.insurancefraud.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class UserController extends BaseController {

    private final UserService userService;

    // ─── Danh sách ────────────────────────────────────────────────────────────

    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            Model model) {

        List<UserResponse> users = (role != null && !role.isBlank())
                ? userService.getUsersByRole(role)
                : userService.getAllUsers();

        // Lọc theo status nếu có
        if (status != null && !status.isBlank()) {
            users = users.stream()
                    .filter(u -> status.equalsIgnoreCase(u.getStatus()))
                    .toList();
        }

        model.addAttribute("users", users);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activePage", "users");
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("breadcrumbParent", "Người dùng");
        model.addAttribute("breadcrumbCurrent", "Danh sách");
        return "users/list";
    }

    // ─── Form tạo mới ─────────────────────────────────────────────────────────

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("userForm", new CreateUserRequest());
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("isEdit", false);
        model.addAttribute("activePage", "users");
        model.addAttribute("breadcrumbParent", "Người dùng");
        model.addAttribute("breadcrumbCurrent", "Tạo mới");
        return "users/form";
    }

    @PostMapping("/create")
    public String createUser(
            @Valid @ModelAttribute("userForm") CreateUserRequest dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "users");
            model.addAttribute("breadcrumbParent", "Người dùng");
            model.addAttribute("breadcrumbCurrent", "Tạo mới");
            return "users/form";
        }

        try {
            userService.createUser(dto);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Tạo tài khoản thành công cho '" + dto.getFullName() + "'");
            return "redirect:/users";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "users");
            model.addAttribute("breadcrumbParent", "Người dùng");
            model.addAttribute("breadcrumbCurrent", "Tạo mới");
            return "users/form";
        }
    }

    // ─── Form chỉnh sửa ───────────────────────────────────────────────────────

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        UserResponse user = userService.getUserById(id);

        UpdateUserRequest form = new UpdateUserRequest();
        form.setFullName(user.getFullName());
        form.setRole(user.getRole());
        form.setDepartment(user.getDepartment());
        form.setPhoneNumber(user.getPhoneNumber());

        model.addAttribute("userForm", form);
        model.addAttribute("targetUser", user);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("isEdit", true);
        model.addAttribute("activePage", "users");
        model.addAttribute("breadcrumbParent", "Người dùng");
        model.addAttribute("breadcrumbCurrent", "Chỉnh sửa");
        return "users/form";
    }

    @PostMapping("/{id}/edit")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute("userForm") UpdateUserRequest dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            UserResponse targetUser = userService.getUserById(id);
            model.addAttribute("targetUser", targetUser);
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "users");
            model.addAttribute("breadcrumbParent", "Người dùng");
            model.addAttribute("breadcrumbCurrent", "Chỉnh sửa");
            return "users/form";
        }

        try {
            userService.updateUser(id, dto, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật tài khoản thành công");
            return "redirect:/users";
        } catch (IllegalArgumentException e) {
            UserResponse targetUser = userService.getUserById(id);
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("targetUser", targetUser);
            model.addAttribute("roles", UserRole.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "users");
            model.addAttribute("breadcrumbParent", "Người dùng");
            model.addAttribute("breadcrumbCurrent", "Chỉnh sửa");
            return "users/form";
        }
    }

    // ─── Khóa / Mở khóa ──────────────────────────────────────────────────────

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            UserResponse updated = userService.toggleStatus(id, userDetails.getUserId());
            String action = updated.isActive() ? "mở khóa" : "khóa";
            redirectAttributes.addFlashAttribute("successMsg",
                    "Đã " + action + " tài khoản '" + updated.getFullName() + "'");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/users";
    }
}
