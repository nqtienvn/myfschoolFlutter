# MyFschool Backend — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement backend REST API for MyFschool electronic student handbook using Java Spring Boot 3.4.x, MySQL, JWT auth, and WebSocket.

**Architecture:** Monolith Spring Boot application with Controller → Service → Repository → Entity layers. JWT for stateless auth. WebSocket for real-time messaging. MapStruct for DTO mapping. Flyway for DB migrations.

**Tech Stack:** Java 21, Spring Boot 3.4.x, MySQL 8.x, Spring Data JPA, Hibernate, jjwt 0.12.x, BCrypt, Spring WebSocket, Jakarta Bean Validation, MapStruct 1.6.x, SpringDoc OpenAPI 3.0, Maven.

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

# Phase 1 — Foundation (Auth + Users + Reference Data)

## 1A: Project Setup + Enums + Common

### Task 1: Spring Boot Project Init

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/MyfSchoolApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`
- Create: `backend/src/main/resources/application-test.yml`

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
        <relativePath/>
    </parent>

    <groupId>vn.edu.fpt</groupId>
    <artifactId>myfschool-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>myfschool-backend</name>
    <description>Backend API for MyFschool Electronic Student Handbook</description>

    <properties>
        <java.version>21</java.version>
        <jjwt.version>0.12.6</jjwt.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
    </properties>

    <dependencies>
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
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.8.6</version>
        </dependency>
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
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
```

- [ ] **Step 4: Create application-test.yml**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MYSQL;DATABASE_TO_LOWER=TRUE
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

- [ ] **Step 5: Create MyfSchoolApplication.java**

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

- [ ] **Step 6: Verify compilation**

```bash
cd backend && mvn compile
```

- [ ] **Step 7: Commit**

```bash
git add backend/
git commit -m "feat: initialize Spring Boot project with Maven"
```

---

### Task 2: Enums + Common DTOs + Exceptions

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
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ApiResponse.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/PagedResponse.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/exception/ResourceNotFoundException.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/exception/BadRequestException.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/exception/UnauthorizedException.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/exception/ForbiddenException.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Consumes: nothing
- Produces: all enums and DTOs used across the application

- [ ] **Step 1: Create all enums** - Copy from spec sections 1A.1

- [ ] **Step 2: Create ApiResponse.java** - Copy from spec section 1A.1

- [ ] **Step 3: Create PagedResponse.java** - Copy from spec section 1A.1

- [ ] **Step 4: Create exception classes** - Copy from spec section 1A.1

- [ ] **Step 5: Verify compilation**

```bash
cd backend && mvn compile
```

- [ ] **Step 6: Commit**

```bash
git commit -m "feat: add enums, DTOs, and exceptions"
```

---

### Task 3: Base Entity + Security Utils

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/BaseEntity.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/util/SecurityUtil.java`

**Interfaces:**
- Consumes: nothing
- Produces: BaseEntity (base for all entities), SecurityUtil

- [ ] **Step 1: Create BaseEntity.java** - Copy from spec section 1A.1

- [ ] **Step 2: Create SecurityUtil.java** - Copy from spec section 1A.1

- [ ] **Step 3: Verify compilation**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add BaseEntity and SecurityUtil"
```

---

## 1B: Security (JWT + Auth Filter)

### Task 4: CustomUserDetails + JwtTokenProvider

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/security/CustomUserDetails.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/security/JwtTokenProvider.java`

**Interfaces:**
- Consumes: UserRole enum
- Produces: CustomUserDetails, JwtTokenProvider

- [ ] **Step 1: Create CustomUserDetails.java** - Copy from spec section 1A.6

- [ ] **Step 2: Create JwtTokenProvider.java** - Copy from spec section 1A.6

- [ ] **Step 3: Verify compilation**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add CustomUserDetails and JwtTokenProvider"
```

---

### Task 5: JwtAuthenticationFilter + UserDetailsService + SecurityConfig

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/security/UserDetailsServiceImpl.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/config/SecurityConfig.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/config/WebConfig.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/config/OpenApiConfig.java`

**Interfaces:**
- Consumes: JwtTokenProvider, CustomUserDetails
- Produces: SecurityConfig, JwtAuthenticationFilter

- [ ] **Step 1: Create JwtAuthenticationFilter.java** - Copy from spec section 1A.6

- [ ] **Step 2: Create UserDetailsServiceImpl.java** - Copy from spec section 1A.6

- [ ] **Step 3: Create SecurityConfig.java** - Copy from spec section 1A.6

- [ ] **Step 4: Create WebConfig.java** - Copy from spec section 1A.6

- [ ] **Step 5: Create OpenApiConfig.java** - Copy from spec section 1A.6

- [ ] **Step 6: Verify compilation**

- [ ] **Step 7: Commit**

```bash
git commit -m "feat: add security filter, UserDetailsService, and configs"
```

---

## 1C: User + Actor Entities + Repositories

### Task 6: User + UserSetting + Parent + Student + Teacher Entities

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/User.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/UserSetting.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Parent.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Student.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Teacher.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/StudentGuardian.java`

**Interfaces:**
- Consumes: BaseEntity, enums
- Produces: 6 entities

- [ ] **Step 1: Create all entities** - Copy from spec section 1A.1

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add User, UserSetting, Parent, Student, Teacher, StudentGuardian entities"
```

---

### Task 7: Actor Repositories

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/UserRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/UserSettingRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ParentRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/StudentRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/TeacherRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/StudentGuardianRepository.java`

**Interfaces:**
- Consumes: entities
- Produces: repositories

- [ ] **Step 1: Create all repositories** - Copy from spec section 1A.3

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add actor repositories"
```

---

### Task 8: Auth DTOs

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/auth/LoginRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/auth/LoginResponse.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/auth/RegisterRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/auth/ChangePasswordRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/auth/UpdateProfileRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/auth/UpdateSettingsRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/auth/UserDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/auth/ParentDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/auth/StudentDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/auth/TeacherDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/auth/UserSettingDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/auth/StudentSummaryDto.java`

**Interfaces:**
- Consumes: enums
- Produces: auth DTOs

- [ ] **Step 1: Create all DTOs** - Copy from spec section 1A.2

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add auth DTOs"
```

---

### Task 9: AuthService

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/AuthService.java`

**Interfaces:**
- Consumes: repositories, JwtTokenProvider
- Produces: AuthService

- [ ] **Step 1: Create AuthService.java** - Copy from spec section 1A.4

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add AuthService"
```

---

### Task 10: Auth + User Controllers

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/AuthController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/UserController.java`

**Interfaces:**
- Consumes: AuthService
- Produces: REST endpoints

- [ ] **Step 1: Create AuthController.java** - Copy from spec section 1A.5

- [ ] **Step 2: Create UserController.java** - Copy from spec section 1A.5

- [ ] **Step 3: Verify compilation**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add Auth and User controllers"
```

---

## 1D: Reference Data (Classes, Subjects, Semesters)

### Task 11: Class + Subject + Semester Entities

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Class.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Subject.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Semester.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/StudentClass.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/ClassSubject.java`

**Interfaces:**
- Consumes: BaseEntity, enums
- Produces: 5 entities

- [ ] **Step 1: Create all entities** - Copy from spec section 1B.1

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add Class, Subject, Semester, StudentClass, ClassSubject entities"
```

---

### Task 12: Reference Repositories

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ClassRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/SubjectRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/SemesterRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/StudentClassRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ClassSubjectRepository.java`

**Interfaces:**
- Consumes: entities
- Produces: repositories

- [ ] **Step 1: Create all repositories** - Copy from spec section 1B.3

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add reference repositories"
```

---

### Task 13: Reference DTOs

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/reference/ClassDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/reference/ClassDetailDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/reference/SubjectDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/reference/SemesterDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/reference/TeacherSummaryDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/reference/CreateClassRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/reference/CreateSubjectRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/reference/CreateSemesterRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/reference/ClassSubjectDto.java`

**Interfaces:**
- Consumes: entities
- Produces: reference DTOs

- [ ] **Step 1: Create all DTOs** - Copy from spec section 1B.2

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add reference DTOs"
```

---

### Task 14: Reference Services

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/ClassService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/SubjectService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/SemesterService.java`

**Interfaces:**
- Consumes: repositories, DTOs
- Produces: services

- [ ] **Step 1: Create ClassService.java** - Copy from spec section 1B.4

- [ ] **Step 2: Create SubjectService.java** - Copy from spec section 1B.4

- [ ] **Step 3: Create SemesterService.java** - Copy from spec section 1B.4

- [ ] **Step 4: Verify compilation**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: add Class, Subject, and Semester services"
```

---

### Task 15: Reference Controllers

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/ClassController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/SubjectController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/SemesterController.java`

**Interfaces:**
- Consumes: services
- Produces: REST endpoints

- [ ] **Step 1: Create ClassController.java** - Copy from spec section 1B.5

- [ ] **Step 2: Create SubjectController.java** - Copy from spec section 1B.5

- [ ] **Step 3: Create SemesterController.java** - Copy from spec section 1B.5

- [ ] **Step 4: Verify compilation**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: add Class, Subject, and Semester controllers"
```

---

### Task 16: Flyway Migration V1

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`

**Interfaces:**
- Consumes: database schema from docs/database.md
- Produces: Flyway migration

- [ ] **Step 1: Create V1__init_schema.sql** - Copy schema from docs/database.md

- [ ] **Step 2: Verify app starts**

```bash
cd backend && mvn spring-boot:run
```

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add Flyway migration V1"
```

---

# Phase 2 — Academic (Grades + Attendance + Leave Requests)

### Task 17: Grade + Attendance + LeaveRequest Entities

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Grade.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/SemesterResult.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Attendance.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/LeaveRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Attachment.java`

**Interfaces:**
- Consumes: BaseEntity, entities from Phase 1
- Produces: 5 entities

- [ ] **Step 1: Create all entities** - Copy from spec phase-2-academic.md

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add Grade, Attendance, LeaveRequest entities"
```

---

### Task 18: Academic Repositories

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/GradeRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/SemesterResultRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/AttendanceRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/LeaveRequestRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/AttachmentRepository.java`

**Interfaces:**
- Consumes: entities
- Produces: repositories

- [ ] **Step 1: Create all repositories** - Copy from spec phase-2-academic.md

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add Grade, Attendance, LeaveRequest repositories"
```

---

### Task 19: Academic DTOs

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/academic/GradeDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/academic/UpdateGradeRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/academic/AttendanceDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/academic/SubmitAttendanceRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/academic/LeaveRequestDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/academic/CreateLeaveRequestRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/academic/ReviewLeaveRequestRequest.java`

**Interfaces:**
- Consumes: entities, enums
- Produces: academic DTOs

- [ ] **Step 1: Create all DTOs** - Copy from spec phase-2-academic.md

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add Grade, Attendance, LeaveRequest DTOs"
```

---

### Task 20: Academic Services

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/GradeService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/AttendanceService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/LeaveRequestService.java`

**Interfaces:**
- Consumes: repositories, DTOs
- Produces: services

- [ ] **Step 1: Create GradeService.java** - Copy from spec phase-2-academic.md

- [ ] **Step 2: Create AttendanceService.java** - Copy from spec phase-2-academic.md

- [ ] **Step 3: Create LeaveRequestService.java** - Copy from spec phase-2-academic.md

- [ ] **Step 4: Verify compilation**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: add Grade, Attendance, LeaveRequest services"
```

---

### Task 21: Academic Controllers

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/GradeController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/AttendanceController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/LeaveRequestController.java`

**Interfaces:**
- Consumes: services
- Produces: REST endpoints

- [ ] **Step 1: Create GradeController.java** - Copy from spec phase-2-academic.md

- [ ] **Step 2: Create AttendanceController.java** - Copy from spec phase-2-academic.md

- [ ] **Step 3: Create LeaveRequestController.java** - Copy from spec phase-2-academic.md

- [ ] **Step 4: Verify compilation**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: add Grade, Attendance, LeaveRequest controllers"
```

---

# Phase 3 — Messaging (WebSocket + Conversations)

### Task 22: Conversation + Message Entities

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Conversation.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/ConversationParticipant.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Message.java`

**Interfaces:**
- Consumes: BaseEntity, User entity
- Produces: 3 entities

- [ ] **Step 1: Create all entities** - Copy from spec phase-3-messaging.md

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add Conversation and Message entities"
```

---

### Task 23: Messaging Repositories

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ConversationRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ConversationParticipantRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageRepository.java`

**Interfaces:**
- Consumes: entities
- Produces: repositories

- [ ] **Step 1: Create all repositories** - Copy from spec phase-3-messaging.md

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add Conversation and Message repositories"
```

---

### Task 24: Messaging DTOs

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/messaging/ConversationDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/messaging/MessageDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/messaging/CreateConversationRequest.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/messaging/SendMessageRequest.java`

**Interfaces:**
- Consumes: entities
- Produces: messaging DTOs

- [ ] **Step 1: Create all DTOs** - Copy from spec phase-3-messaging.md

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add Conversation and Message DTOs"
```

---

### Task 25: WebSocket Config + Handler

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/config/WebSocketConfig.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatWebSocketHandler.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/WebSocketSessionManager.java`

**Interfaces:**
- Consumes: JwtTokenProvider
- Produces: WebSocket components

- [ ] **Step 1: Create WebSocketConfig.java** - Copy from spec phase-3-messaging.md

- [ ] **Step 2: Create ChatWebSocketHandler.java** - Copy from spec phase-3-messaging.md

- [ ] **Step 3: Create WebSocketSessionManager.java** - Copy from spec phase-3-messaging.md

- [ ] **Step 4: Verify compilation**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: add WebSocket config and handler"
```

---

### Task 26: Messaging Service + Controller

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/ConversationService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/MessageService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/ConversationController.java`

**Interfaces:**
- Consumes: repositories, DTOs, WebSocket
- Produces: services, controllers

- [ ] **Step 1: Create ConversationService.java** - Copy from spec phase-3-messaging.md

- [ ] **Step 2: Create MessageService.java** - Copy from spec phase-3-messaging.md

- [ ] **Step 3: Create ConversationController.java** - Copy from spec phase-3-messaging.md

- [ ] **Step 4: Verify compilation**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: add Conversation and Message services and controller"
```

---

# Phase 4 — Notifications + Announcements

### Task 27: Notification + Announcement Entities

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Notification.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Announcement.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/AnnouncementClass.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/AnnouncementRead.java`

**Interfaces:**
- Consumes: BaseEntity, User, Class entities
- Produces: 4 entities

- [ ] **Step 1: Create all entities** - Copy from spec phase-4-notifications.md

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add Notification and Announcement entities"
```

---

### Task 28: Notification Repositories

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/NotificationRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/AnnouncementRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/AnnouncementClassRepository.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/AnnouncementReadRepository.java`

**Interfaces:**
- Consumes: entities
- Produces: repositories

- [ ] **Step 1: Create all repositories** - Copy from spec phase-4-notifications.md

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add Notification and Announcement repositories"
```

---

### Task 29: Notification DTOs

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/notification/NotificationDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/notification/AnnouncementDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/notification/CreateAnnouncementRequest.java`

**Interfaces:**
- Consumes: entities, enums
- Produces: notification DTOs

- [ ] **Step 1: Create all DTOs** - Copy from spec phase-4-notifications.md

- [ ] **Step 2: Verify compilation**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add Notification and Announcement DTOs"
```

---

### Task 30: Notification Service + Controller

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/NotificationService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/AnnouncementService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/NotificationController.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/AnnouncementController.java`

**Interfaces:**
- Consumes: repositories, DTOs
- Produces: services, controllers

- [ ] **Step 1: Create NotificationService.java** - Copy from spec phase-4-notifications.md

- [ ] **Step 2: Create AnnouncementService.java** - Copy from spec phase-4-notifications.md

- [ ] **Step 3: Create NotificationController.java** - Copy from spec phase-4-notifications.md

- [ ] **Step 4: Create AnnouncementController.java** - Copy from spec phase-4-notifications.md

- [ ] **Step 5: Verify compilation**

- [ ] **Step 6: Commit**

```bash
git commit -m "feat: add Notification and Announcement services and controllers"
```

---

# Phase 5 — Reports + Statistics

### Task 31: Report DTOs + Services

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/report/DashboardDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/report/GradeStatisticsDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/report/AttendanceStatisticsDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/ReportService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/ReportController.java`

**Interfaces:**
- Consumes: repositories
- Produces: report DTOs, service, controller

- [ ] **Step 1: Create DTOs** - Copy from spec phase-5-reports.md

- [ ] **Step 2: Create ReportService.java** - Copy from spec phase-5-reports.md

- [ ] **Step 3: Create ReportController.java** - Copy from spec phase-5-reports.md

- [ ] **Step 4: Verify compilation**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: add Report service and controller"
```

---

### Task 32: Final Integration Test

**Files:**
- Create: `backend/src/test/java/vn/edu/fpt/myfschool/MyfSchoolApplicationTests.java`

**Interfaces:**
- Consumes: all services and controllers
- Produces: test

- [ ] **Step 1: Create test** - Verify context loads

- [ ] **Step 2: Run tests**

```bash
cd backend && mvn test
```

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add final integration test"
```

---

## Summary

| Phase | Tasks | Files Created |
|-------|-------|---------------|
| 1: Foundation | 1-16 | 50+ |
| 2: Academic | 17-21 | 20+ |
| 3: Messaging | 22-26 | 15+ |
| 4: Notifications | 27-30 | 15+ |
| 5: Reports | 31-32 | 6 |
| **Total** | **32** | **~106** |

## Key Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven dependencies |
| `application.yml` | Configuration |
| `V1__init_schema.sql` | Database schema |
| `BaseEntity.java` | Base entity with ID, timestamps |
| `JwtTokenProvider.java` | JWT token generation |
| `SecurityConfig.java` | Security configuration |
| `WebSocketConfig.java` | WebSocket configuration |
| `GlobalExceptionHandler.java` | Exception handling |

## Implementation Notes

1. **Phase 1 is critical** - All other phases depend on it
2. **Entities use Lombok** - Less boilerplate
3. **DTOs are records** - Immutable and thread-safe
4. **Repositories use Spring Data** - Minimal implementation needed
5. **Services are transactional** - @Transactional annotation
6. **Controllers return ResponseEntity** - Standard HTTP responses
7. **WebSocket is separate** - Different protocol, separate config
8. **Flyway manages schema** - Version-controlled migrations

## Next Steps

After implementation:
1. Run `mvn spring-boot:run` to start the app
2. Access Swagger UI at `http://localhost:8080/swagger-ui.html`
3. Test endpoints using Swagger or Postman
4. Verify WebSocket connection
5. Run tests to ensure everything works
