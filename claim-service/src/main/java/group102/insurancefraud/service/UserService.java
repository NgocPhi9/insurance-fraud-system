package group102.insurancefraud.service;

import group102.insurancefraud.dto.request.CreateUserRequest;
import group102.insurancefraud.dto.request.UpdateUserRequest;
import group102.insurancefraud.dto.response.UserResponse;
import group102.insurancefraud.entity.User;
import group102.insurancefraud.enums.UserRole;
import group102.insurancefraud.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Lấy danh sách ───────────────────────────────────────────────────────

    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByOrderByFullNameAsc()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    public List<UserResponse> getUsersByRole(String role) {
        return userRepository.findByRoleIgnoreCase(role)
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = findUserOrThrow(id);
        return UserResponse.from(user);
    }

    // ─── Tạo tài khoản ───────────────────────────────────────────────────────

    @Transactional
    public UserResponse createUser(CreateUserRequest dto) {
        // Validate email unique
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email '" + dto.getEmail() + "' đã được sử dụng");
        }

        // Validate role
        validateRole(dto.getRole());

        User user = User.builder()
                .fullName(dto.getFullName().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole().toUpperCase())
                .department(dto.getDepartment())
                .phoneNumber(dto.getPhoneNumber())
                .status("ACTIVE")
                .build();

        userRepository.save(user);
        log.info("Tạo tài khoản mới: email={}, role={}", user.getEmail(), user.getRole());
        return UserResponse.from(user);
    }

    // ─── Cập nhật thông tin / Role ────────────────────────────────────────────

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest dto, Long currentUserId) {
        User user = findUserOrThrow(id);

        // Không cho admin tự đổi role của mình
        if (id.equals(currentUserId) && !user.getRole().equalsIgnoreCase(dto.getRole())) {
            throw new IllegalArgumentException("Bạn không thể tự thay đổi role của chính mình");
        }

        validateRole(dto.getRole());

        user.setFullName(dto.getFullName().trim());
        user.setRole(dto.getRole().toUpperCase());
        user.setDepartment(dto.getDepartment());
        user.setPhoneNumber(dto.getPhoneNumber());

        userRepository.save(user);
        log.info("Cập nhật tài khoản: userId={}, role={}", id, user.getRole());
        return UserResponse.from(user);
    }

    // ─── Khóa / Mở khóa ──────────────────────────────────────────────────────

    @Transactional
    public UserResponse toggleStatus(Long id, Long currentUserId) {
        // Không cho admin tự khóa mình
        if (id.equals(currentUserId)) {
            throw new IllegalArgumentException("Bạn không thể tự khóa tài khoản của chính mình");
        }

        User user = findUserOrThrow(id);
        String newStatus = "ACTIVE".equals(user.getStatus()) ? "INACTIVE" : "ACTIVE";
        user.setStatus(newStatus);

        userRepository.save(user);
        log.info("Thay đổi trạng thái: userId={}, status={}", id, newStatus);
        return UserResponse.from(user);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với id=" + id));
    }

    private void validateRole(String role) {
        boolean valid = Arrays.stream(UserRole.values())
                .anyMatch(r -> r.name().equalsIgnoreCase(role));
        if (!valid) {
            throw new IllegalArgumentException("Role không hợp lệ: " + role);
        }
    }
}
