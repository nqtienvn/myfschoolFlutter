package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.myfschool.common.enums.LeaveShift;
import vn.edu.fpt.myfschool.common.enums.LeaveStatus;

@Entity
@Table(name = "leave_requests")
@Data
@EqualsAndHashCode(callSuper = true)
public class LeaveRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_lr_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_lr_parent"))
    private Parent parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_lr_class"))
    private SchoolClass cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by",
                foreignKey = @ForeignKey(name = "fk_lr_teacher"))
    private Teacher approvedBy;

    @Column(nullable = false)
    private LocalDate dateFrom;

    @Column(nullable = false)
    private LocalDate dateTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveShift shift;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String response;

    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "leaveRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "leaveRequest", fetch = FetchType.LAZY)
    private List<Attendance> attendanceRecords = new ArrayList<>();
}
