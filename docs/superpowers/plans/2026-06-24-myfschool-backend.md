# MyFschool Backend — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete REST API backend for MyFschool electronic student handbook using Java Spring Boot, connecting to MySQL with JWT authentication.

**Architecture:** Monolith Spring Boot 3.4.x application following clean layered architecture: Controller → Service → Repository → Entity. JWT-based stateless authentication. WebSocket for real-time messaging. MapStruct for DTO mapping. Flyway for DB migrations.

**Tech Stack:** Java 21, Spring Boot 3.4.x, MySQL 8.x, Spring Data JPA, Hibernate, jjwt 0.12.x, BCrypt, Spring WebSocket, Jakarta Bean Validation, MapStruct 1.6.x, SpringDoc OpenAPI 3.0, Maven

## Global Constraints

- Java 21 LTS minimum
- Spring Boot 3.4.x
- MySQL 8.x with utf8mb4_unicode_ci, InnoDB engine
- All DB columns: BIGINT UNSIGNED for IDs, TIMESTAMP for created_at/updated_at
- API response format: `ApiResponse<T>` wrapper with success/message/data/timestamp
- JWT expiry: 24 hours configurable
- BCrypt for password hashing
- Enum values stored as STRING in JPA (not ordinal)
- All timestamps in Asia/Ho_Chi_Minh timezone
- Vietnamese for UI text/comments, English for code identifiers

---

# Phase 1 — Foundation (26 tasks)

## P1A: Project Scaffolding + Enums + Common

### Task 1: Spring Boot Project Init

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/MyfSchoolApplication.java`
- Create: `backend/src/main/resources/application.yml`

**Interfaces:**
- Consumes: nothing
- Produces: runnable Spring Boot app

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.5</version>
    </parent>

    <groupId>vn.edu.fpt</groupId>
    <artifactId>myfschool-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>myfschool-backend</name>
    <description>MyFschool Electronic Student Handbook Backend</description>

    <properties>
        <java.version>21</java.version>
        <jjwt.version>0.12.6</jjwt.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- OpenAPI (Swagger) -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.8.6</version>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>${lombok-mapstruct-binding.version}</version>
                        </path>
                    </annotationProcessorPaths>
                    <compilerArgs>
                        <arg>-Amapstruct.defaultComponentModel=spring</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create application.yml**

```yaml
spring:
  application:
    name: myfschool-backend

  profiles:
    active: dev

  jpa:
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080
  servlet:
    context-path: /

jwt:
  secret: myfschool-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm
  expiration: 86400000  # 24 hours in ms

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method

logging:
  level:
    vn.edu.fpt.myfschool: DEBUG
    org.hibernate.SQL: WARN
```

- [ ] **Step 3: Create application-dev.yml**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/myfschool?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
```

- [ ] **Step 4: Create application-test.yml (for tests with H2)**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  flyway:
    enabled: false
```

- [ ] **Step 5: Create main application class**

```java
package vn.edu.fpt.myfschool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyfSchoolApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyfSchoolApplication.class, args);
    }
}
```

- [ ] **Step 6: Create directory structure**

```bash
mkdir -p backend/src/main/java/vn/edu/fpt/myfschool/{config,security,common/{enums,dto,exception,util},entity,repository,mapper,service,controller,websocket}
mkdir -p backend/src/main/resources/db/migration
mkdir -p backend/src/test/java/vn/edu/fpt/myfschool
```

- [ ] **Step 7: Verify build compiles**

```bash
cd backend && mvn compile
```
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add backend/
git commit -m "feat: init Spring Boot project with Maven, dependencies, and configs"
```

---

### Task 2: Enums

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/UserRole.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/UserStatus.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/AttendanceStatus.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/LeaveShift.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/LeaveStatus.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/Theme.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/Language.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/Shift.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/BillStatus.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/PaymentStatus.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/TargetRole.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/Relationship.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/ClubStatus.java`

**Interfaces:**
- Consumes: nothing
- Produces: all enums used across the entire project

- [ ] **Step 1: Create all 13 enum files**

```java
// UserRole.java
package vn.edu.fpt.myfschool.common.enums;
public enum UserRole { PARENT, STUDENT, TEACHER }

// UserStatus.java
package vn.edu.fpt.myfschool.common.enums;
public enum UserStatus { ACTIVE, INACTIVE, LOCKED }

// AttendanceStatus.java
package vn.edu.fpt.myfschool.common.enums;
public enum AttendanceStatus { PRESENT, LATE, ABSENT_WITH_LEAVE, ABSENT_WITHOUT_LEAVE }

// LeaveShift.java
package vn.edu.fpt.myfschool.common.enums;
public enum LeaveShift { FULL_DAY, MORNING, AFTERNOON }

// LeaveStatus.java
package vn.edu.fpt.myfschool.common.enums;
public enum LeaveStatus { PENDING, APPROVED, REJECTED }

// Theme.java
package vn.edu.fpt.myfschool.common.enums;
public enum Theme { LIGHT, DARK }

// Language.java
package vn.edu.fpt.myfschool.common.enums;
public enum Language { VI, EN }

// Shift.java
package vn.edu.fpt.myfschool.common.enums;
public enum Shift { MORNING, AFTERNOON }

// BillStatus.java
package vn.edu.fpt.myfschool.common.enums;
public enum BillStatus { UNPAID, PROCESSING, PAID }

// PaymentStatus.java
package vn.edu.fpt.myfschool.common.enums;
public enum PaymentStatus { PENDING, SUCCESS, FAILED }

// TargetRole.java
package vn.edu.fpt.myfschool.common.enums;
public enum TargetRole { PARENT, STUDENT, ALL }

// Relationship.java
package vn.edu.fpt.myfschool.common.enums;
public enum Relationship { FATHER, MOTHER, GUARDIAN }

// ClubStatus.java
package vn.edu.fpt.myfschool.common.enums;
public enum ClubStatus { REGISTERED, CANCELLED }
```

- [ ] **Step 2: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/common/enums/
git commit -m "feat: add all domain enums"
```

---

### Task 3: Common DTOs + Exceptions

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ApiResponse.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/PagedResponse.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ErrorResponse.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/exception/ResourceNotFoundException.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/exception/BadRequestException.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/exception/UnauthorizedException.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/exception/ForbiddenException.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/exception/ConflictException.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Consumes: nothing
- Produces: ApiResponse<T>, PagedResponse<T>, exception classes used by all controllers/services

- [ ] **Step 1: Create ApiResponse**

```java
package vn.edu.fpt.myfschool.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Thành công")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
```

- [ ] **Step 2: Create PagedResponse**

```java
package vn.edu.fpt.myfschool.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
```

- [ ] **Step 3: Create all exception classes**

```java
// ResourceNotFoundException.java
package vn.edu.fpt.myfschool.common.exception;
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s không tìm thấy với %s: '%s'", resource, field, value));
    }
}

// BadRequestException.java
package vn.edu.fpt.myfschool.common.exception;
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}

// UnauthorizedException.java
package vn.edu.fpt.myfschool.common.exception;
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) { super(message); }
}

// ForbiddenException.java
package vn.edu.fpt.myfschool.common.exception;
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}

// ConflictException.java
package vn.edu.fpt.myfschool.common.exception;
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
```

- [ ] **Step 4: Create GlobalExceptionHandler**

```java
package vn.edu.fpt.myfschool.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Số điện thoại hoặc mật khẩu không đúng"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bạn không có quyền truy cập tài nguyên này"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new java.util.HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Lỗi validation")
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi hệ thống: " + ex.getMessage()));
    }
}
```

- [ ] **Step 5: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/common/
git commit -m "feat: add common DTOs, exceptions, and global exception handler"
```

---

### Task 4: BaseEntity + SecurityUtil

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/BaseEntity.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/util/SecurityUtil.java`

**Interfaces:**
- Consumes: nothing
- Produces: BaseEntity (base for all 27 entities), SecurityUtil (used by all services)

- [ ] **Step 1: Create BaseEntity**

```java
package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: Create SecurityUtil**

```java
package vn.edu.fpt.myfschool.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.security.CustomUserDetails;

public class SecurityUtil {

    private SecurityUtil() {}

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new vn.edu.fpt.myfschool.common.exception.UnauthorizedException(
                "Chưa đăng nhập");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUserId();
    }

    public static UserRole getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getRole();
    }

    public static CustomUserDetails getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) auth.getPrincipal();
    }
}
```

- [ ] **Step 3: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/entity/BaseEntity.java
git add backend/src/main/java/vn/edu/fpt/myfschool/common/util/SecurityUtil.java
git commit -m "feat: add BaseEntity and SecurityUtil"
```

---

## P1B: Security (JWT + Auth Filter)

### Task 5: CustomUserDetails + JwtTokenProvider

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/security/CustomUserDetails.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/security/JwtTokenProvider.java`

**Interfaces:**
- Consumes: UserRole enum
- Produces: CustomUserDetails (used by SecurityUtil, Filter), JwtTokenProvider (used by AuthService, Filter)

- [ ] **Step 1: Create CustomUserDetails**

```java
package vn.edu.fpt.myfschool.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import java.util.Collection;
import java.util.List;

@Data
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {
    private Long userId;
    private String phone;
    private String password;
    private String name;
    private UserRole role;
    private boolean enabled;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() { return phone; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
}
```

- [ ] **Step 2: Create JwtTokenProvider**

```java
package vn.edu.fpt.myfschool.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.exception.UnauthorizedException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, UserRole role, String name) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .claim("name", name)
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

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
```

- [ ] **Step 3: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/security/
git commit -m "feat: add CustomUserDetails and JwtTokenProvider"
```

---

### Task 6: JWT Auth Filter + UserDetailsService + SecurityConfig

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/security/UserDetailsServiceImpl.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/config/SecurityConfig.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/config/WebConfig.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/config/OpenApiConfig.java`

**Interfaces:**
- Consumes: JwtTokenProvider, CustomUserDetails (from Task 5)
- Produces: SecurityConfig (protects all endpoints), JwtAuthenticationFilter (auth pipeline)

- [ ] **Step 1: Create JwtAuthenticationFilter**

```java
package vn.edu.fpt.myfschool.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            Long userId = tokenProvider.getUserIdFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserById(userId);
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 2: Create UserDetailsService**

```java
package vn.edu.fpt.myfschool.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.edu.fpt.myfschool.entity.User;
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
```

- [ ] **Step 3: Create SecurityConfig**

```java
package vn.edu.fpt.myfschool.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.edu.fpt.myfschool.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/ws/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 4: Create WebConfig (CORS)**

```java
package vn.edu.fpt.myfschool.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000", "http://localhost:8081")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

- [ ] **Step 5: Create OpenApiConfig**

```java
package vn.edu.fpt.myfschool.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new io.swagger.v3.oas.models.info.Info()
                .title("MyFschool API")
                .version("1.0.0")
                .description("Backend API cho ứng dụng Sổ liên lạc điện tử MyFschool"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

- [ ] **Step 6: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/security/
git add backend/src/main/java/vn/edu/fpt/myfschool/config/
git commit -m "feat: add JWT filter, UserDetailsService, SecurityConfig, WebConfig, OpenAPI"
```

---

## P1C: Auth Entities + Service + Controller

### Task 7: User + UserSetting + Parent + Student + Teacher Entities

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/User.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/UserSetting.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Parent.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Student.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Teacher.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/StudentGuardian.java`

**Interfaces:**
- Consumes: BaseEntity (Task 4), all enums (Task 2)
- Produces: 6 entities used by repositories, services, controllers across all phases

- [ ] **Step 1: Create all 6 entities** — copy exactly from spec phase-1-foundation.md sections 1A.1 and 1B.1 (User, UserSetting, Parent, Student, Teacher, StudentGuardian)

- [ ] **Step 2: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/entity/
git commit -m "feat: add User, UserSetting, Parent, Student, Teacher, StudentGuardian entities"
```

---

### Task 8: Auth Repositories

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/UserRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/UserSettingRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ParentRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/StudentRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/TeacherRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/StudentGuardianRepository.java`

**Interfaces:**
- Consumes: User, UserSetting, Parent, Student, Teacher, StudentGuardian entities (Task 7)
- Produces: Repository interfaces used by AuthService, UserService, and all downstream services

- [ ] **Step 1: Create all 6 repositories** — copy from spec phase-1-foundation.md section 1A.3

- [ ] **Step 2: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/repository/
git commit -m "feat: add auth and actor repositories"
```

---

### Task 9: Auth DTOs

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/LoginRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/LoginResponse.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/RegisterRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ChangePasswordRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/UpdateProfileRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/UpdateSettingsRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/UserDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ParentDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/StudentDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/TeacherDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/UserSettingDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/StudentSummaryDto.java`

**Interfaces:**
- Consumes: UserRole, Theme, Language enums (Task 2)
- Produces: DTOs used by AuthController, UserController, downstream controllers

- [ ] **Step 1: Create all DTO records** — copy from spec phase-1-foundation.md section 1A.2

- [ ] **Step 2: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/common/dto/
git commit -m "feat: add auth and user DTOs"
```

---

### Task 10: AuthService

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/AuthService.java`

**Interfaces:**
- Consumes: UserRepository, ParentRepository, StudentRepository, TeacherRepository, UserSettingRepository, StudentGuardianRepository, JwtTokenProvider, PasswordEncoder
- Produces: login(), register(), getProfile(), changePassword(), updateProfile(), updateSettings(), linkGuardianStudent()

- [ ] **Step 1: Create AuthService** — full implementation from spec phase-1-foundation.md section 1A.4

- [ ] **Step 2: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/service/AuthService.java
git commit -m "feat: add AuthService with login, register, profile management"
```

---

### Task 11: Auth + User Controllers

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/AuthController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/UserController.java`

**Interfaces:**
- Consumes: AuthService (Task 10), SecurityUtil (Task 4)
- Produces: 7 API endpoints: login, register, getProfile, updateProfile, changePassword, getSettings, updateSettings

- [ ] **Step 1: Create AuthController** — from spec phase-1-foundation.md section 1A.5

- [ ] **Step 2: Create UserController** — from spec phase-1-foundation.md section 1A.5

- [ ] **Step 3: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 4: Start app and test with Swagger**

```bash
cd backend && mvn spring-boot:run
```
Open http://localhost:8080/swagger-ui.html — verify Auth endpoints appear.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/AuthController.java
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/UserController.java
git commit -m "feat: add AuthController and UserController with 7 endpoints"
```

---

### Task 12: Flyway Migration V1

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`

**Interfaces:**
- Consumes: full schema from docs/database.md
- Produces: 27 tables ready for JPA validation

- [ ] **Step 1: Create V1__init_schema.sql** — copy all CREATE TABLE statements from docs/database.md sections 3.2–3.9, including all indexes and foreign keys

- [ ] **Step 2: Verify app starts with validation**

```bash
cd backend && mvn spring-boot:run
```
Expected: Spring Boot starts, Hibernate validates schema against migration.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/
git commit -m "feat: add Flyway migration V1 with full 27-table schema"
```

---

## P1D: Reference Data CRUD

### Task 13: Class + Subject + Semester Entities

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Class.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Subject.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Semester.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/StudentClass.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/ClassSubject.java`

**Interfaces:**
- Consumes: BaseEntity (Task 4), User/Student/Teacher entities (Task 7)
- Produces: 5 entities used across all phases

- [ ] **Step 1: Create all 5 entities** — from spec phase-1-foundation.md section 1B.1

- [ ] **Step 2: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/entity/
git commit -m "feat: add Class, Subject, Semester, StudentClass, ClassSubject entities"
```

---

### Task 14: Reference Repositories

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ClassRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/SubjectRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/SemesterRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/StudentClassRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ClassSubjectRepository.java`

**Interfaces:**
- Consumes: Class, Subject, Semester, StudentClass, ClassSubject entities (Task 13)
- Produces: Repository interfaces for ClassService, SubjectService, SemesterService

- [ ] **Step 1: Create all 5 repositories** — from spec phase-1-foundation.md section 1B.3

- [ ] **Step 2: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/repository/
git commit -m "feat: add Class, Subject, Semester, ClassSubject repositories"
```

---

### Task 15: Reference DTOs + Mappers

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ClassDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ClassDetailDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ClassSubjectDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/SubjectDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/SemesterDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/TeacherSummaryDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/CreateClassRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/CreateSubjectRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/CreateSemesterRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/CreateClassSubjectRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/LinkGuardianRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/mapper/ClassMapper.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/mapper/SubjectMapper.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/mapper/SemesterMapper.java`

**Interfaces:**
- Consumes: Class, Subject, Semester, ClassSubject entities (Task 13)
- Produces: DTOs + Mappers for ClassService, SubjectService, SemesterService

- [ ] **Step 1: Create all DTOs** — from spec phase-1-foundation.md section 1B.2

- [ ] **Step 2: Create MapStruct mappers**

```java
// ClassMapper.java
package vn.edu.fpt.myfschool.mapper;

import org.mapstruct.*;
import vn.edu.fpt.myfschool.entity.Class;
import vn.edu.fpt.myfschool.common.dto.ClassDto;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ClassMapper {
    @Mapping(target = "studentCount", expression = "java(cls.getStudents() != null ? cls.getStudents().size() : 0)")
    ClassDto toDto(Class cls);

    List<ClassDto> toDtoList(List<Class> classes);
}

// SubjectMapper.java
package vn.edu.fpt.myfschool.mapper;

import org.mapstruct.Mapper;
import vn.edu.fpt.myfschool.entity.Subject;
import vn.edu.fpt.myfschool.common.dto.SubjectDto;

@Mapper(componentModel = "spring")
public interface SubjectMapper {
    SubjectDto toDto(Subject subject);
}

// SemesterMapper.java
package vn.edu.fpt.myfschool.mapper;

import org.mapstruct.Mapper;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.common.dto.SemesterDto;

@Mapper(componentModel = "spring")
public interface SemesterMapper {
    SemesterDto toDto(Semester semester);
}
```

- [ ] **Step 3: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/common/dto/
git add backend/src/main/java/vn/edu/fpt/myfschool/mapper/
git commit -m "feat: add reference data DTOs and MapStruct mappers"
```

---

### Task 16: ClassService + ClassController

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/ClassService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/ClassController.java`

**Interfaces:**
- Consumes: ClassRepository, ClassSubjectRepository, StudentRepository (Tasks 8, 14), ClassMapper (Task 15), DTOs (Task 15)
- Produces: 7 endpoints: list, detail, create, update, delete, students, assignSubject, removeSubject

- [ ] **Step 1: Create ClassService** — from spec phase-1-foundation.md section 1B.4

- [ ] **Step 2: Create ClassController** — from spec phase-1-foundation.md section 1B.5

- [ ] **Step 3: Verify compile**

```bash
cd backend && mvn compile
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/service/ClassService.java
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/ClassController.java
git commit -m "feat: add ClassService and ClassController with 7 endpoints"
```

---

### Task 17: SubjectService + SubjectController

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/SubjectService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/SubjectController.java`

**Interfaces:**
- Consumes: SubjectRepository, ClassSubjectRepository (Task 14), SubjectMapper (Task 15)
- Produces: 5 endpoints: list, get, create, update, delete

- [ ] **Step 1: Create SubjectService** — from spec phase-1-foundation.md section 1B.4

- [ ] **Step 2: Create SubjectController** — from spec phase-1-foundation.md section 1B.5

- [ ] **Step 3: Verify compile + commit**

```bash
cd backend && mvn compile
git add backend/src/main/java/vn/edu/fpt/myfschool/service/SubjectService.java
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/SubjectController.java
git commit -m "feat: add SubjectService and SubjectController"
```

---

### Task 18: SemesterService + SemesterController

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/SemesterService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/SemesterController.java`

**Interfaces:**
- Consumes: SemesterRepository (Task 14), SemesterMapper (Task 15)
- Produces: 6 endpoints: list, current, get, create, update, setCurrent

- [ ] **Step 1: Create SemesterService** — from spec phase-1-foundation.md section 1B.4

- [ ] **Step 2: Create SemesterController** — from spec phase-1-foundation.md section 1B.5

- [ ] **Step 3: Verify compile + commit**

```bash
cd backend && mvn compile
git add backend/src/main/java/vn/edu/fpt/myfschool/service/SemesterService.java
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/SemesterController.java
git commit -m "feat: add SemesterService and SemesterController"
```

---

### Task 19: Seed Data + Integration Test

**Files:**
- Create: `backend/src/main/resources/data.sql`
- Create: `backend/src/test/java/vn/edu/fpt/myfschool/AuthIntegrationTest.java`

**Interfaces:**
- Consumes: all Phase 1 entities, services, controllers
- Produces: verified working auth + CRUD flow

- [ ] **Step 1: Create data.sql** — sample data from docs/database.md section 6

- [ ] **Step 2: Write integration test**

```java
package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone": "0901234001", "password": "password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void login_withInvalidPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone": "0901234001", "password": "wrongpassword"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_newUser_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone": "0999999999", "password": "test1234",
                     "name": "Test User", "role": "PARENT"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").isNotEmpty());
    }
}
```

- [ ] **Step 3: Run tests**

```bash
cd backend && mvn test
```
Expected: All 3 tests PASS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/data.sql
git add backend/src/test/java/vn/edu/fpt/myfschool/AuthIntegrationTest.java
git commit -m "feat: add seed data and auth integration tests"
```

---

## P1E: Sample Data SQL Importer

### Task 20: Data Importer for Dev

**Files:**
- Create: `backend/src/main/resources/import.sql` (disabled by default, run manually)

**Interfaces:**
- Consumes: V1 schema (Task 12)
- Produces: Dev database populated with sample data

- [ ] **Step 1: Create import.sql** — comprehensive INSERT statements from docs/database.md section 6 (all sample data)
- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/import.sql
git commit -m "feat: add dev sample data SQL import script"
```

---

## Phase 1 Final Verification

### Task 21: Phase 1 Smoke Test

- [ ] **Step 1: Start the app**

```bash
cd backend && mvn spring-boot:run
```

- [ ] **Step 2: Test all Phase 1 endpoints manually via Swagger**

```
POST /api/auth/register → register a user
POST /api/auth/login → get JWT
GET /api/user/profile → with Bearer token
POST /api/classes → create a class
POST /api/subjects → create a subject
POST /api/semesters → create a semester
```

- [ ] **Step 3: Run all tests**

```bash
cd backend && mvn test
```

- [ ] **Step 4: Commit Phase 1 complete**

```bash
git commit --allow-empty -m "feat: Phase 1 Foundation complete — Auth + Users + Reference CRUD"
```

---

# Phase 2 — Academic (10 tasks)

### Task 22: Grade + SemesterResult + Attendance + LeaveRequest Entities

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Grade.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/SemesterResult.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Attendance.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/LeaveRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Attachment.java`

**Interfaces:**
- Consumes: BaseEntity (Task 4), Student/Teacher/Class/Subject/Semester entities (Tasks 7, 13)
- Produces: 5 entities used by GradeService, AttendanceService, LeaveRequestService

- [ ] **Step 1: Create all 5 entities** — from spec phase-2-academic.md sections 2A.1, 2B.1, 2C.1, 2D.1, and phase-4-business.md section 4D.1

- [ ] **Step 2: Verify compile + commit**

```bash
cd backend && mvn compile
git add backend/src/main/java/vn/edu/fpt/myfschool/entity/
git commit -m "feat: add Grade, SemesterResult, Attendance, LeaveRequest, Attachment entities"
```

---

### Task 23: Grade + Attendance + LeaveRequest Repositories

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/GradeRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/SemesterResultRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/AttendanceRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/LeaveRequestRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/AttachmentRepository.java`

**Interfaces:**
- Consumes: Grade, SemesterResult, Attendance, LeaveRequest, Attachment entities (Task 22)
- Produces: Repositories for GradeService, AttendanceService, LeaveRequestService

- [ ] **Step 1: Create all 5 repositories** — from spec phase-2-academic.md sections 2A.3, 2B.3, 2C.3, 2D.3

- [ ] **Step 2: Verify compile + commit**

```bash
cd backend && mvn compile
git add backend/src/main/java/vn/edu/fpt/myfschool/repository/
git commit -m "feat: add Grade, SemesterResult, Attendance, LeaveRequest repositories"
```

---

### Task 24: Phase 2 DTOs

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/GradeDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/StudentSemesterGradesDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/SubjectGradesDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/StudentGradeRowDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/SimulationResultDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/UpdateGradeRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/BatchGradeUpdateRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/GradeEntry.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/SemesterResultDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ClassRankingDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ClassRankEntryDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/AttendanceDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/DailyAttendanceDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/AttendanceEntryDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/SubmitAttendanceRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/AttendanceEntry.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/AttendanceStatsDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/AttendanceLogDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/LeaveRequestDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/CreateLeaveRequestRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ReviewLeaveRequestRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/AttachmentDto.java`

**Interfaces:**
- Consumes: all Phase 1 + Phase 2 entities
- Produces: DTOs used by Phase 2 controllers and services

- [ ] **Step 1: Create all DTOs** — from spec phase-2-academic.md sections 2A.2, 2B.2, 2C.2, 2D.2

- [ ] **Step 2: Verify compile + commit**

```bash
cd backend && mvn compile
git add backend/src/main/java/vn/edu/fpt/myfschool/common/dto/
git commit -m "feat: add Phase 2 DTOs for grades, attendance, leave requests"
```

---

### Task 25: GradeCalculator + GradeService

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/util/GradeCalculator.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/GradeService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/SemesterResultService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/mapper/GradeMapper.java`

**Interfaces:**
- Consumes: GradeRepository, SemesterResultRepository, StudentRepository, SubjectRepository, SemesterRepository, AttendanceRepository
- Produces: getStudentGrades(), getSubjectGrades(), updateGrade(), batchUpdateGrades(), simulateGrades(), recalculate()

- [ ] **Step 1: Create GradeCalculator**

```java
package vn.edu.fpt.myfschool.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GradeCalculator {

    private GradeCalculator() {}

    /**
     * Calculate subject average.
     * Formula: (oral + quiz15m*2 + midTerm*3 + final*4) / 10
     * Null values treated as 0.
     */
    public static BigDecimal calculateAverage(BigDecimal oral, BigDecimal quiz15m,
                                               BigDecimal midTerm, BigDecimal finalScore) {
        BigDecimal o = oral != null ? oral : BigDecimal.ZERO;
        BigDecimal q = quiz15m != null ? quiz15m : BigDecimal.ZERO;
        BigDecimal m = midTerm != null ? midTerm : BigDecimal.ZERO;
        BigDecimal f = finalScore != null ? finalScore : BigDecimal.ZERO;

        BigDecimal weighted = o.add(q.multiply(BigDecimal.valueOf(2)))
                .add(m.multiply(BigDecimal.valueOf(3)))
                .add(f.multiply(BigDecimal.valueOf(4)));

        return weighted.divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
    }

    /**
     * Determine academic ability from GPA
     */
    public static String getAcademicAbility(BigDecimal gpa) {
        if (gpa == null) return null;
        if (gpa.compareTo(BigDecimal.valueOf(8.0)) >= 0) return "Giỏi";
        if (gpa.compareTo(BigDecimal.valueOf(6.5)) >= 0) return "Khá";
        if (gpa.compareTo(BigDecimal.valueOf(5.0)) >= 0) return "Trung bình";
        return "Yếu";
    }

    /**
     * Determine conduct from attendance rate (percentage)
     */
    public static String getConduct(double attendanceRate) {
        if (attendanceRate >= 90) return "Tốt";
        if (attendanceRate >= 75) return "Khá";
        if (attendanceRate >= 60) return "Trung bình";
        return "Yếu";
    }
}
```

- [ ] **Step 2: Create GradeMapper**

```java
package vn.edu.fpt.myfschool.mapper;

import org.mapstruct.*;
import vn.edu.fpt.myfschool.entity.Grade;
import vn.edu.fpt.myfschool.common.dto.GradeDto;

@Mapper(componentModel = "spring")
public interface GradeMapper {
    @Mapping(target = "subjectId", source = "subject.id")
    @Mapping(target = "subjectName", source = "subject.name")
    @Mapping(target = "subjectCode", source = "subject.code")
    @Mapping(target = "finalScore", source = "final")
    GradeDto toDto(Grade grade);
}
```

- [ ] **Step 3: Create GradeService** — from spec phase-2-academic.md section 2A.4

- [ ] **Step 4: Create SemesterResultService** — from spec phase-2-academic.md section 2B.4

- [ ] **Step 5: Verify compile + commit**

```bash
cd backend && mvn compile
git add backend/src/main/java/vn/edu/fpt/myfschool/
git commit -m "feat: add GradeCalculator, GradeService, SemesterResultService"
```

---

### Task 26: AttendanceService + LeaveRequestService

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/AttendanceService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/LeaveRequestService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/mapper/AttendanceMapper.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/mapper/LeaveRequestMapper.java`

**Interfaces:**
- Consumes: AttendanceRepository, LeaveRequestRepository, StudentRepository, TeacherRepository, ClassRepository, SemesterRepository, SemesterResultService, NotificationService (stub)
- Produces: getDailyAttendance(), submitAttendance(), autoUpdateForApprovedLeave(), createLeaveRequest(), approveLeaveRequest(), rejectLeaveRequest()

- [ ] **Step 1: Create AttendanceMapper**

```java
package vn.edu.fpt.myfschool.mapper;

import org.mapstruct.*;
import vn.edu.fpt.myfschool.entity.Attendance;
import vn.edu.fpt.myfschool.common.dto.AttendanceDto;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student.user.name")
    @Mapping(target = "studentCode", source = "student.studentCode")
    @Mapping(target = "classId", source = "cls.id")
    @Mapping(target = "className", source = "cls.name")
    @Mapping(target = "teacherName", source = "teacher.user.name")
    AttendanceDto toDto(Attendance attendance);
}
```

- [ ] **Step 2: Create LeaveRequestMapper**

```java
package vn.edu.fpt.myfschool.mapper;

import org.mapstruct.*;
import vn.edu.fpt.myfschool.entity.LeaveRequest;
import vn.edu.fpt.myfschool.common.dto.LeaveRequestDto;

@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student.user.name")
    @Mapping(target = "studentCode", source = "student.studentCode")
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.user.name")
    @Mapping(target = "classId", source = "cls.id")
    @Mapping(target = "className", source = "cls.name")
    @Mapping(target = "approvedById", source = "approvedBy.id")
    @Mapping(target = "approvedByName", expression = "java(lr.getApprovedBy() != null ? lr.getApprovedBy().getUser().getName() : null)")
    LeaveRequestDto toDto(LeaveRequest lr);
}
```

- [ ] **Step 3: Create AttendanceService** — from spec phase-2-academic.md section 2C.4

- [ ] **Step 4: Create LeaveRequestService** — from spec phase-2-academic.md section 2D.4

- [ ] **Step 5: Verify compile + commit**

```bash
cd backend && mvn compile
git add backend/src/main/java/vn/edu/fpt/myfschool/
git commit -m "feat: add AttendanceService, LeaveRequestService with auto-approve flow"
```

---

### Task 27: Phase 2 Controllers

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/GradeController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/SemesterResultController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/AttendanceController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/LeaveRequestController.java`

**Interfaces:**
- Consumes: GradeService, SemesterResultService, AttendanceService, LeaveRequestService, SecurityUtil
- Produces: 20 API endpoints for grades, attendance, leave requests

- [ ] **Step 1: Create all 4 controllers** — from spec phase-2-academic.md section 2D.5

- [ ] **Step 2: Verify compile + commit**

```bash
cd backend && mvn compile
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/
git commit -m "feat: add Grade, SemesterResult, Attendance, LeaveRequest controllers"
```

---

### Task 28: Phase 2 Integration Tests

**Files:**
- Create: `backend/src/test/java/vn/edu/fpt/myfschool/GradeIntegrationTest.java`
- Create: `backend/src/test/java/vn/edu/fpt/myfschool/LeaveRequestIntegrationTest.java`

- [ ] **Step 1: Write grade tests** — test update grade, batch update, simulation
- [ ] **Step 2: Write leave request tests** — test create, approve (auto-update attendance), reject
- [ ] **Step 3: Run tests**

```bash
cd backend && mvn test
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/
git commit -m "test: add Phase 2 integration tests for grades and leave requests"
```

---

# Phase 3 — Communication (10 tasks)

### Task 29: Conversation + Message + Announcement + Notification Entities

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Conversation.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/ConversationParticipant.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Message.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Announcement.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/AnnouncementClass.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/AnnouncementRead.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Notification.java`

**Interfaces:**
- Consumes: BaseEntity (Task 4), User/Teacher/Class entities (Tasks 7, 13)
- Produces: 7 entities for messaging, announcements, notifications

- [ ] **Step 1: Create all 7 entities** — from spec phase-3-communication.md sections 3A.1, 3C.1, 3D.1

- [ ] **Step 2: Verify compile + commit**

```bash
cd backend && mvn compile
git add backend/src/main/java/vn/edu/fpt/myfschool/entity/
git commit -m "feat: add Conversation, Message, Announcement, Notification entities"
```

---

### Task 30: Phase 3 Repositories + DTOs

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ConversationRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ConversationParticipantRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/AnnouncementRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/AnnouncementClassRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/AnnouncementReadRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/NotificationRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/` — all Phase 3 DTOs (ConversationDto, MessageDto, AnnouncementDto, NotificationDto, etc.)

**Interfaces:**
- Consumes: Phase 3 entities (Task 29)
- Produces: Repositories + DTOs for Phase 3 services

- [ ] **Step 1: Create all 7 repositories** — from spec phase-3-communication.md sections 3A.3, 3C.3, 3D.3

- [ ] **Step 2: Create all Phase 3 DTOs** — from spec phase-3-communication.md sections 3A.2, 3C.2, 3D.2

- [ ] **Step 3: Verify compile + commit**

```bash
cd backend && mvn compile
git commit -m "feat: add Phase 3 repositories and DTOs"
```

---

### Task 31: NotificationService + AnnouncementService

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/NotificationService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/AnnouncementService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/mapper/NotificationMapper.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/mapper/AnnouncementMapper.java`

**Interfaces:**
- Consumes: NotificationRepository, AnnouncementRepository, AnnouncementClassRepository, AnnouncementReadRepository, WebSocketSessionManager (stub initially)
- Produces: createNotification(), getNotifications(), getUnreadCount(), createAnnouncement(), markAsRead()

- [ ] **Step 1: Create NotificationService** — from spec phase-3-communication.md section 3D.4

- [ ] **Step 2: Create AnnouncementService** — from spec phase-3-communication.md section 3C.4

- [ ] **Step 3: Verify compile + commit**

```bash
cd backend && mvn compile
git commit -m "feat: add NotificationService and AnnouncementService"
```

---

### Task 32: ConversationService + MessageService

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/ConversationService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/MessageService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/mapper/MessageMapper.java`

**Interfaces:**
- Consumes: ConversationRepository, ConversationParticipantRepository, MessageRepository, UserRepository, WebSocketSessionManager
- Produces: getConversations(), createOrFindConversation(), getConversationDetail(), sendMessage(), markAsRead(), getTotalUnreadCount()

- [ ] **Step 1: Create MessageMapper**

```java
package vn.edu.fpt.myfschool.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.fpt.myfschool.entity.Message;
import vn.edu.fpt.myfschool.common.dto.MessageDto;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderName", source = "sender.name")
    MessageDto toDto(Message message);
}
```

- [ ] **Step 2: Create ConversationService** — from spec phase-3-communication.md section 3A.4

- [ ] **Step 3: Create MessageService** — from spec phase-3-communication.md section 3A.4

- [ ] **Step 4: Verify compile + commit**

```bash
cd backend && mvn compile
git commit -m "feat: add ConversationService and MessageService"
```

---

### Task 33: WebSocket Infrastructure

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/config/WebSocketConfig.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/WebSocketAuthInterceptor.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/WebSocketSessionManager.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatWebSocketHandler.java`

**Interfaces:**
- Consumes: JwtTokenProvider (Task 5), ConversationService (Task 32), NotificationService (Task 31)
- Produces: WebSocket endpoint at ws://host/ws/chat?token=JWT

- [ ] **Step 1: Create WebSocketConfig** — from spec phase-3-communication.md section 3B.2

- [ ] **Step 2: Create WebSocketAuthInterceptor** — from spec phase-3-communication.md section 3B.3

- [ ] **Step 3: Create WebSocketSessionManager** — from spec phase-3-communication.md section 3B.4

- [ ] **Step 4: Create ChatWebSocketHandler** — from spec phase-3-communication.md section 3B.5

- [ ] **Step 5: Verify compile + commit**

```bash
cd backend && mvn compile
git add backend/src/main/java/vn/edu/fpt/myfschool/config/WebSocketConfig.java
git add backend/src/main/java/vn/edu/fpt/myfschool/websocket/
git commit -m "feat: add WebSocket infrastructure for real-time messaging"
```

---

### Task 34: Phase 3 Controllers

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/ConversationController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/AnnouncementController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/NotificationController.java`

**Interfaces:**
- Consumes: ConversationService, MessageService, AnnouncementService, NotificationService, SecurityUtil
- Produces: 16 API endpoints

- [ ] **Step 1: Create all 3 controllers** — from spec phase-3-communication.md section 3E

- [ ] **Step 2: Verify compile + commit**

```bash
cd backend && mvn compile
git commit -m "feat: add Conversation, Announcement, Notification controllers"
```

---

# Phase 4 — Business (8 tasks)

### Task 35: TuitionBill + PaymentTransaction + ClubRegistration + Attachment Entities + Repos

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/TuitionBill.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/PaymentTransaction.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/ClubRegistration.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/TuitionBillRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/PaymentTransactionRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ClubRegistrationRepository.java`

**Interfaces:**
- Consumes: BaseEntity, Student, Class, Semester, User entities
- Produces: 3 entities + 3 repositories for Phase 4 services

- [ ] **Step 1: Create entities** — from spec phase-4-business.md sections 4A.1, 4B.1, 4C.1

- [ ] **Step 2: Create repositories** — from spec phase-4-business.md sections 4A.3, 4B.3, 4C.3

- [ ] **Step 3: Verify compile + commit**

```bash
cd backend && mvn compile
git commit -m "feat: add TuitionBill, PaymentTransaction, ClubRegistration entities and repos"
```

---

### Task 36: Phase 4 DTOs + Services

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/` — all Phase 4 DTOs
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/TuitionBillService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/PaymentTransactionService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/ClubRegistrationService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/AttachmentService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/mapper/TuitionBillMapper.java`

- [ ] **Step 1: Create all DTOs** — from spec phase-4-business.md sections 4A.2, 4B.2, 4C.2, 4D.2

- [ ] **Step 2: Create all 4 services** — from spec phase-4-business.md sections 4A.4, 4B.4, 4C.4, 4D.4

- [ ] **Step 3: Verify compile + commit**

```bash
cd backend && mvn compile
git commit -m "feat: add TuitionBill, Payment, Club, Attachment services and DTOs"
```

---

### Task 37: Phase 4 Controllers

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/TuitionBillController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/ClubRegistrationController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/AttachmentController.java`

- [ ] **Step 1: Create all 3 controllers** — from spec phase-4-business.md section 4E

- [ ] **Step 2: Verify compile + commit**

```bash
cd backend && mvn compile
git commit -m "feat: add Tuition, Club, Attachment controllers (19 endpoints)"
```

---

# Phase 5 — Schedule + Dashboard (6 tasks)

### Task 38: Schedule Entity + Repository + DTOs

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Schedule.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ScheduleRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ScheduleDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ClassScheduleDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/DayScheduleDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/TeacherScheduleDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/WeekScheduleDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/CreateScheduleRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/BatchCreateScheduleRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ScheduleEntry.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/mapper/ScheduleMapper.java`

- [ ] **Step 1: Create Schedule entity** — from spec phase-5-schedule-reports.md section 5A.1

- [ ] **Step 2: Create ScheduleRepository** — from spec phase-5-schedule-reports.md section 5A.3

- [ ] **Step 3: Create all DTOs** — from spec phase-5-schedule-reports.md section 5A.2

- [ ] **Step 4: Create ScheduleMapper**

```java
package vn.edu.fpt.myfschool.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.fpt.myfschool.entity.Schedule;
import vn.edu.fpt.myfschool.common.dto.ScheduleDto;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {

    Map<Integer, String> DAY_NAMES = Map.of(
        1, "Chủ nhật", 2, "Thứ 2", 3, "Thứ 3", 4, "Thứ 4",
        5, "Thứ 5", 6, "Thứ 6", 7, "Thứ 7"
    );

    @Mapping(target = "classId", source = "cls.id")
    @Mapping(target = "className", source = "cls.name")
    @Mapping(target = "subjectId", source = "subject.id")
    @Mapping(target = "subjectName", source = "subject.name")
    @Mapping(target = "subjectCode", source = "subject.code")
    @Mapping(target = "teacherId", source = "teacher.id")
    @Mapping(target = "teacherName", source = "teacher.user.name")
    @Mapping(target = "semesterId", source = "semester.id")
    @Mapping(target = "semesterName", source = "semester.name")
    @Mapping(target = "dayOfWeekName", expression = "java(DAY_NAMES.get(schedule.getDayOfWeek()))")
    ScheduleDto toDto(Schedule schedule);
}
```

- [ ] **Step 5: Verify compile + commit**

```bash
cd backend && mvn compile
git commit -m "feat: add Schedule entity, repository, DTOs, and mapper"
```

---

### Task 39: ScheduleService + ScheduleController

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/ScheduleService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/ScheduleController.java`

- [ ] **Step 1: Create ScheduleService** — from spec phase-5-schedule-reports.md section 5A.4

- [ ] **Step 2: Create ScheduleController** — from spec phase-5-schedule-reports.md section 5C

- [ ] **Step 3: Verify compile + commit**

```bash
cd backend && mvn compile
git commit -m "feat: add ScheduleService and ScheduleController (9 endpoints)"
```

---

### Task 40: Dashboard DTOs + Service + Controller

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/TeacherDashboardDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ParentDashboardDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/StudentDashboardDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/DashboardStatsDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/DashboardStudentStatsDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ClassStatsDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/SubjectStatsDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/DashboardService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/DashboardController.java`

- [ ] **Step 1: Create all DTOs** — from spec phase-5-schedule-reports.md section 5B.1

- [ ] **Step 2: Create DashboardService** — from spec phase-5-schedule-reports.md section 5B.3

- [ ] **Step 3: Create DashboardController** — from spec phase-5-schedule-reports.md section 5B.2

- [ ] **Step 4: Verify compile + commit**

```bash
cd backend && mvn compile
git commit -m "feat: add DashboardService and DashboardController (4 endpoints)"
```

---

### Task 41: Full Integration Test Suite

**Files:**
- Create: `backend/src/test/java/vn/edu/fpt/myfschool/FullIntegrationTest.java`

- [ ] **Step 1: Write end-to-end test covering all 5 phases**

```java
package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FullIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fullFlow_register_login_getProfile() throws Exception {
        // Register
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone": "0987654321", "password": "test1234",
                     "name": "Full Test", "role": "TEACHER",
                     "employeeCode": "GV999", "department": "Test Dept"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andReturn().getResponse().getContentAsString();

        // Extract token
        String token = com.jayway.jsonpath.JsonPath.read(registerResponse, "$.data.token");

        // Get profile
        mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Full Test"))
            .andExpect(jsonPath("$.data.teacherProfile.employeeCode").value("GV999"));
    }
}
```

- [ ] **Step 2: Run all tests**

```bash
cd backend && mvn test
```

- [ ] **Step 3: Commit**

```bash
git commit -m "test: add full integration test suite"
```

---

### Task 42: Sample Data + Final Smoke Test

**Files:**
- Update: `backend/src/main/resources/data.sql` — complete sample data for all 27 tables

- [ ] **Step 1: Update data.sql** with full sample data from docs/database.md section 6

- [ ] **Step 2: Start app with MySQL and verify Swagger**

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- [ ] **Step 3: Test all 94 endpoints via Swagger UI**

- [ ] **Step 4: Final commit**

```bash
git commit -m "feat: complete MyFschool backend — all 94 endpoints across 5 phases"
```

---

## Summary

| Phase | Tasks | Endpoints | Key Features |
|-------|-------|-----------|-------------|
| P1 Foundation | 1-21 | 26 | Auth, JWT, RBAC, Users, Classes, Subjects, Semesters |
| P2 Academic | 22-28 | 20 | Grades, GPA, Attendance, Leave Requests |
| P3 Communication | 29-34 | 16 + WS | Conversations, Messages, Announcements, WebSocket |
| P4 Business | 35-37 | 19 | Tuition, Payments, Clubs, Attachments |
| P5 Schedule+Reports | 38-42 | 13 | Schedule CRUD, Dashboard, Statistics |
| **TOTAL** | **42 tasks** | **~94 + WS** | **27 tables, complete backend** |

### Parallel Execution Map

```
Task 1 (Project init)
  └──→ Task 2 (Enums) + Task 3 (Common DTOs) — parallel
         └──→ Task 4 (BaseEntity) + Task 5 (JWT) — parallel
                └──→ Task 6 (Security Config) — depends on both
                       └──→ Task 7 (Entities) + Task 8 (Repos) — parallel
                              └──→ Task 9 (DTOs) → Task 10 (AuthService) → Task 11 (Controllers)
                                     └──→ Task 12 (Migration) → Tasks 13-21 (Reference CRUD)
                                                                                    └──→ Phase 2-5 sequential
```

Phase 2-5 can each be parallelized internally (entities/repos/dtos before services/controllers).

---

*Plan written to `docs/superpowers/plans/2026-06-24-myfschool-backend.md`*
