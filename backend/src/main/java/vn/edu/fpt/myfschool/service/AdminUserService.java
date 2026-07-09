package vn.edu.fpt.myfschool.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.myfschool.common.dto.AdminUserDto;
import vn.edu.fpt.myfschool.common.dto.CreateTeacherAccountRequest;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;

public interface AdminUserService {
    Page<AdminUserDto> listUsers(UserRole role, UserStatus status, String keyword, int page, int size);
    AdminUserDto updateUserStatus(Long userId, UserStatus status);
    AdminUserDto createTeacherAccount(CreateTeacherAccountRequest request);
}
