package vn.edu.fpt.myfschool.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.security.JwtTokenProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    UserDto getProfile(Long userId);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserDto updateProfile(Long userId, UpdateProfileRequest request);

    UserSettingDto updateSettings(Long userId, UpdateSettingsRequest request);

    void linkGuardianStudent(Long parentId, Long studentId,
                                     vn.edu.fpt.myfschool.common.enums.Relationship relationship);
}
