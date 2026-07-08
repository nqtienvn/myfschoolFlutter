package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "audit_logs")
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLog extends BaseEntity {

    private Long userId;

    @Column(length = 100)
    private String username;

    @Column(length = 50)
    private String role;

    @Column(nullable = false, length = 10)
    private String httpMethod;

    @Column(nullable = false, length = 500)
    private String uri;

    @Column(length = 45)
    private String clientIp;

    @Lob
    private String requestParams;

    @Lob
    private String requestBody;

    @Lob
    private String responseBody;

    private Integer statusCode;

    @Column(length = 1000)
    private String errorMessage;

    private Long executionTimeMs;
}
