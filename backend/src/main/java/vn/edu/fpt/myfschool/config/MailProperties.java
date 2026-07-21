package vn.edu.fpt.myfschool.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class MailProperties {
    private String provider = "FAKE";
    private String from = "no-reply@myfschool.edu.vn";
    private boolean asyncEnabled = true;
    private int maxAttempts = 3;
    private long retryDelayMillis = 1000;
}
