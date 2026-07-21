package vn.edu.fpt.myfschool.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.util.EnumSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app.password-reset")
@Getter
@Setter
public class PasswordResetProperties {
    private boolean enabled;
    private String frontendUrl = "http://localhost:5173/reset-password";
    private String tokenSecret = "local-password-reset-secret-change-me";
    private int tokenTtlMinutes = 15;
    private int accountHourlyLimit = 3;
    private int ipHourlyLimit = 10;
    private long minimumResponseMillis = 150;
    private Set<UserRole> allowedRoles = EnumSet.of(
            UserRole.PARENT,
            UserRole.STUDENT,
            UserRole.TEACHER
    );
}
