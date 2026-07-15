package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.MeetingAttendance;
import vn.edu.fpt.myfschool.common.enums.MeetingResponse;

import java.time.LocalDateTime;

@Entity
@Table(name = "parent_meeting_participants", uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "guardian_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class ParentMeetingParticipant extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "meeting_id", nullable = false)
    private ParentMeeting meeting;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "guardian_id", nullable = false)
    private Parent guardian;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private MeetingResponse response = MeetingResponse.PENDING;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private MeetingAttendance attendance = MeetingAttendance.UNKNOWN;
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
