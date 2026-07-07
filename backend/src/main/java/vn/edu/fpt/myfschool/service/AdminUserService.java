package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.AdminUserDto;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import java.util.List;

public interface AdminUserService {
    List<AdminUserDto> listUsers(UserRole role, UserStatus status, String keyword);
    AdminUserDto updateUserStatus(Long userId, UserStatus status);
}
