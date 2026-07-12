package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.AttendanceCorrectionStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_correction_requests")
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceCorrectionRequest extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Shift shift;

    @Lob
    @Column(name = "proposed_entries", nullable = false, columnDefinition = "LONGTEXT")
    private String proposedEntries;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceCorrectionStatus status = AttendanceCorrectionStatus.PENDING;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
