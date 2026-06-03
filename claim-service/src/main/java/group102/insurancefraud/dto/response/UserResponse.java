package group102.insurancefraud.dto.response;

import group102.insurancefraud.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private String department;
    private String phoneNumber;
    private String status;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .department(user.getDepartment())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .build();
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
