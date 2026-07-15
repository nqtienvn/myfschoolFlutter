package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="student_score_audits") @Getter @Setter @NoArgsConstructor
public class StudentScoreAudit extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="student_score_id",nullable=false) private StudentScore studentScore;
    private BigDecimal oldScore;
    private BigDecimal newScore;
    @Column(length=255) private String oldComment;
    @Column(length=255) private String newComment;
    private Boolean oldIsGraded;
    private Boolean newIsGraded;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="changed_by",nullable=false) private User changedBy;
    @Column(length=500) private String reason;
    @Column(nullable=false) private LocalDateTime changedAt;
}
