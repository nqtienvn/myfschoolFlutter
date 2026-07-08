package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AdminUserDto;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.UserRepository;
import vn.edu.fpt.myfschool.service.AdminUserService;

@Service("adminUserService")
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public Page<AdminUserDto> listUsers(UserRole role, UserStatus status, String keyword, int page, int size) {
        String query = keyword == null || keyword.isBlank() ? null : keyword.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        PageRequest pageable = PageRequest.of(
            safePage,
            safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return userRepository.searchAdminUsers(role, status, query, pageable).map(this::toDto);
    }

    @Override
    public AdminUserDto updateUserStatus(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setStatus(status);
        user = userRepository.save(user);
        return toDto(user);
    }

    private AdminUserDto toDto(User user) {
        return new AdminUserDto(
            user.getId(), user.getPhone(), user.getName(), user.getEmail(),
            user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
