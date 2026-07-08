package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import java.time.LocalDateTime;

public record AdminUserDto(
        Long id,
        String phone,
        String name,
        String email,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt) {
}
