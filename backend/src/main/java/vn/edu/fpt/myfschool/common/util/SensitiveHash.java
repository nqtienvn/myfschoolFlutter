package vn.edu.fpt.myfschool.common.util;

import org.springframework.stereotype.Component;
import vn.edu.fpt.myfschool.config.PasswordResetProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class SensitiveHash {
    private final byte[] secret;

    public SensitiveHash(PasswordResetProperties properties) {
        this.secret = properties.getTokenSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(secret);
            digest.update((byte) 0);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
