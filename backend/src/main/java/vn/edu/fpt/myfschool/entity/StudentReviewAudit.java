package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_review_audits")
@Getter
@Setter
@NoArgsConstructor
public class StudentReviewAudit extends BaseEntity {
    @Column(name = "entity_type", nullable = false, length = 40) private String entityType;
    @Column(name = "entity_id", nullable = false) private Long entityId;
    @Column(name = "old_value_json", columnDefinition = "LONGTEXT") private String oldValueJson;
    @Column(name = "new_value_json", columnDefinition = "LONGTEXT") private String newValueJson;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "changed_by", nullable = false) private User changedBy;
    @Column(nullable = false, length = 500) private String reason;
    @Column(name = "changed_at", nullable = false) private LocalDateTime changedAt;
}
