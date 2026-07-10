package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    UserDto getProfile(Long userId);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserDto updateProfile(Long userId, UpdateProfileRequest request);

    void linkGuardianStudent(Long parentId, Long studentId,
                                     vn.edu.fpt.myfschool.common.enums.Relationship relationship);
}
