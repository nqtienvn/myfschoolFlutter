package vn.edu.fpt.myfschool.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.exception.UnauthorizedException;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.myfschool.entity.AuditLog;
import vn.edu.fpt.myfschool.security.CustomUserDetails;
import vn.edu.fpt.myfschool.service.AuditLogService;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private static final int MAX_BODY_LENGTH = 4000;
    private static final Set<String> SENSITIVE_FIELD_PARTS = Set.of("password", "token", "secret", "credential");

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long startedAt = System.currentTimeMillis();
        HttpServletRequest request = currentRequest();
        try {
            Object response = joinPoint.proceed();
            save(joinPoint, request, response, null, System.currentTimeMillis() - startedAt);
            return response;
        } catch (Throwable ex) {
            save(joinPoint, request, null, ex, System.currentTimeMillis() - startedAt);
            throw ex;
        }
    }

    private void save(ProceedingJoinPoint joinPoint, HttpServletRequest request, Object response, Throwable error, long executionTimeMs) {
        if (request == null) return;

        AuditLog auditLog = new AuditLog();
        auditLog.setHttpMethod(request.getMethod());
        auditLog.setUri(request.getRequestURI());
        auditLog.setClientIp(clientIp(request));
        auditLog.setRequestParams(toJson(request.getParameterMap()));
        auditLog.setRequestBody(toJson(args(joinPoint)));
        auditLog.setResponseBody(error == null ? toJson(response) : null);
        auditLog.setStatusCode(statusCode(response, error));
        auditLog.setErrorMessage(error == null ? null : truncate(error.getMessage()));
        auditLog.setExecutionTimeMs(executionTimeMs);
        fillUser(auditLog);

        log.info("{} {} status={} time={}ms user={}", auditLog.getHttpMethod(), auditLog.getUri(), auditLog.getStatusCode(), executionTimeMs, auditLog.getUsername());
        auditLogService.save(auditLog);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private Object[] args(ProceedingJoinPoint joinPoint) {
        String[] names = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Object[] values = joinPoint.getArgs();
        Object[] out = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = safeArg(names != null && i < names.length ? names[i] : "arg" + i, values[i]);
        }
        return out;
    }

    private Object safeArg(String name, Object value) {
        if (value == null) return null;
        if (value instanceof MultipartFile file) return Map.of(name, file.getOriginalFilename());
        String type = value.getClass().getName();
        if (type.startsWith("jakarta.servlet.") || type.startsWith("org.springframework.")) return Map.of(name, type);
        return Map.of(name, value);
    }

    private String toJson(Object value) {
        try {
            JsonNode node = objectMapper.valueToTree(value);
            mask(node);
            return truncate(objectMapper.writeValueAsString(node));
        } catch (IllegalArgumentException | JsonProcessingException ex) {
            return truncate(String.valueOf(value));
        }
    }

    private void mask(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            object.fieldNames().forEachRemaining(field -> {
                String normalized = field.toLowerCase(java.util.Locale.ROOT);
                if (SENSITIVE_FIELD_PARTS.stream().anyMatch(normalized::contains)) {
                    object.put(field, "***");
                } else {
                    mask(object.get(field));
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::mask);
        }
    }

    private int statusCode(Object response, Throwable error) {
        if (error instanceof ResourceNotFoundException) return 404;
        if (error instanceof BadRequestException || error instanceof IllegalArgumentException) return 400;
        if (error instanceof ConflictException) return 409;
        if (error instanceof UnauthorizedException) return 401;
        if (error instanceof ForbiddenException) return 403;
        if (error != null) return 500;
        if (response instanceof ResponseEntity<?> entity) return entity.getStatusCode().value();
        return 200;
    }

    private void fillUser(AuditLog auditLog) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return;
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails user) {
            auditLog.setUserId(user.getUserId());
            auditLog.setUsername(user.getUsername());
            auditLog.setRole(user.getRole().name());
        } else {
            auditLog.setUsername(String.valueOf(principal));
            auditLog.setRole(Arrays.toString(auth.getAuthorities().toArray()));
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_BODY_LENGTH) return value;
        return value.substring(0, MAX_BODY_LENGTH);
    }
}
