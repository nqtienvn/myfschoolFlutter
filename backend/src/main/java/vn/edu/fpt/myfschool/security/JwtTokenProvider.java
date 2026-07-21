package vn.edu.fpt.myfschool.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, UserRole role, String name, LocalDateTime credentialsUpdatedAt) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .claim("name", name)
                .claim("credentialsUpdatedAt", credentialVersion(credentialsUpdatedAt))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public UserRole getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        return UserRole.valueOf(claims.get("role", String.class));
    }

    public String getNameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("name", String.class);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public boolean matchesCredentialsVersion(String token, LocalDateTime credentialsUpdatedAt) {
        Claims claims = getClaims(token);
        Number tokenVersion = claims.get("credentialsUpdatedAt", Number.class);
        return tokenVersion != null && tokenVersion.longValue() == credentialVersion(credentialsUpdatedAt);
    }

    private long credentialVersion(LocalDateTime credentialsUpdatedAt) {
        if (credentialsUpdatedAt == null) return 0L;
        return credentialsUpdatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
