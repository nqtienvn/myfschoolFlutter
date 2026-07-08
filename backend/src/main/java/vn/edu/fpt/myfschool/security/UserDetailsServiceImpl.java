package vn.edu.fpt.myfschool.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.edu.fpt.myfschool.controller.entity.User;
import vn.edu.fpt.myfschool.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Không tìm thấy tài khoản với số điện thoại: " + phone));
        return toUserDetails(user);
    }

    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Không tìm thấy tài khoản với id: " + userId));
        return toUserDetails(user);
    }

    private CustomUserDetails toUserDetails(User user) {
        return new CustomUserDetails(
            user.getId(),
            user.getPhone(),
            user.getPassword(),
            user.getName(),
            user.getRole(),
            user.getStatus().name().equals("ACTIVE")
        );
    }
}
