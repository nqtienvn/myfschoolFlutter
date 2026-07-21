package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.PasswordResetConfirmRequest;
import vn.edu.fpt.myfschool.common.dto.PasswordResetValidationResponse;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.util.SensitiveHash;
import vn.edu.fpt.myfschool.config.PasswordResetProperties;
import vn.edu.fpt.myfschool.entity.PasswordResetAttempt;
import vn.edu.fpt.myfschool.entity.PasswordResetToken;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.PasswordResetAttemptRepository;
import vn.edu.fpt.myfschool.repository.PasswordResetTokenRepository;
import vn.edu.fpt.myfschool.repository.UserRepository;
import vn.edu.fpt.myfschool.service.MailDeliveryService;
import vn.edu.fpt.myfschool.service.PasswordResetService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetProperties properties;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final SensitiveHash sensitiveHash;
    private final PasswordEncoder passwordEncoder;
    private final MailDeliveryService mailDeliveryService;

    @Override
    @Transactional
    public void request(String phone, String requestedIp) {
        String normalizedPhone = phone.trim();
        String phoneHash = sensitiveHash.sha256(normalizedPhone);
        String ip = normalizeIp(requestedIp);
        LocalDateTime now = LocalDateTime.now();

        // Luôn thực hiện công việc mật mã để giảm chênh lệch thời gian theo trạng thái tài khoản.
        String rawToken = generateToken();
        String tokenHash = sensitiveHash.sha256(rawToken);
        if (!properties.isEnabled()) return;

        User candidate = userRepository.findByPhone(normalizedPhone).orElse(null);
        LocalDateTime oneHourAgo = now.minusHours(1);
        boolean rateLimited = attemptRepository.countByPhoneHashAndCreatedAtAfter(phoneHash, oneHourAgo)
                >= properties.getAccountHourlyLimit()
                || attemptRepository.countByRequestedIpAndCreatedAtAfter(ip, oneHourAgo)
                >= properties.getIpHourlyLimit();

        PasswordResetAttempt attempt = new PasswordResetAttempt();
        attempt.setUser(candidate);
        attempt.setPhoneHash(phoneHash);
        attempt.setRequestedIp(ip);
        attemptRepository.save(attempt);

        if (rateLimited || !eligible(candidate)) return;

        User user = userRepository.findByIdForUpdate(candidate.getId()).orElse(null);
        if (!eligible(user)) return;

        tokenRepository.invalidateUnusedTokens(user.getId(), now);
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(now.plusMinutes(properties.getTokenTtlMinutes()));
        resetToken.setRequestedIp(ip);
        tokenRepository.save(resetToken);

        String baseUrl = properties.getFrontendUrl().split("#", 2)[0];
        mailDeliveryService.sendPasswordResetAfterCommit(user.getEmail(), baseUrl + "#token=" + rawToken);
    }

    @Override
    @Transactional(readOnly = true)
    public PasswordResetValidationResponse validate(String rawToken) {
        if (!properties.isEnabled()) return new PasswordResetValidationResponse(false, "DISABLED");
        PasswordResetToken token = tokenRepository.findByTokenHash(sensitiveHash.sha256(rawToken)).orElse(null);
        if (token == null) return new PasswordResetValidationResponse(false, "INVALID");
        if (token.getUsedAt() != null) return new PasswordResetValidationResponse(false, "USED");
        if (!token.getExpiresAt().isAfter(LocalDateTime.now())) {
            return new PasswordResetValidationResponse(false, "EXPIRED");
        }
        return new PasswordResetValidationResponse(true, "VALID");
    }

    @Override
    @Transactional
    public void confirm(PasswordResetConfirmRequest request) {
        if (!properties.isEnabled()) throw invalidToken();
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        PasswordResetToken token = tokenRepository.findByTokenHashForUpdate(sensitiveHash.sha256(request.token()))
                .orElseThrow(this::invalidToken);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) throw invalidToken();

        User user = token.getUser();
        if (!properties.getAllowedRoles().contains(user.getRole())) throw invalidToken();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setCredentialsUpdatedAt(now);
        user.setMustChangePassword(false);
        userRepository.save(user);

        token.setUsedAt(now);
        tokenRepository.saveAndFlush(token);
        tokenRepository.invalidateUnusedTokens(user.getId(), now);

        if (user.getEmail() != null && user.getEmailVerifiedAt() != null) {
            mailDeliveryService.sendPasswordChangedAfterCommit(user.getEmail(), user.getName());
        }
    }

    private boolean eligible(User user) {
        return user != null
                && properties.getAllowedRoles().contains(user.getRole())
                && user.getEmail() != null
                && user.getEmailVerifiedAt() != null;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeIp(String requestedIp) {
        if (requestedIp == null || requestedIp.isBlank()) return "unknown";
        String value = requestedIp.trim();
        return value.length() <= 45 ? value : value.substring(0, 45);
    }

    private BadRequestException invalidToken() {
        return new BadRequestException("Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
    }
}
