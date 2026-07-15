package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.myfschool.entity.ParentMeetingParticipant;

import java.util.List;
import java.util.Optional;

public interface ParentMeetingParticipantRepository extends JpaRepository<ParentMeetingParticipant, Long> {
    Optional<ParentMeetingParticipant> findByMeetingIdAndGuardianId(Long meetingId, Long guardianId);

    @Query("SELECT p FROM ParentMeetingParticipant p WHERE p.guardian.user.id = :userId " +
            "AND p.meeting.academicYear.id = :academicYearId AND p.meeting.semester.id = :semesterId " +
            "ORDER BY p.meeting.startsAt DESC")
    List<ParentMeetingParticipant> findForGuardian(
            @Param("userId") Long userId,
            @Param("academicYearId") Long academicYearId,
            @Param("semesterId") Long semesterId);

    @Query("SELECT p FROM ParentMeetingParticipant p WHERE p.meeting.id = :meetingId AND p.guardian.user.id = :userId")
    Optional<ParentMeetingParticipant> findByMeetingAndGuardianUser(
            @Param("meetingId") Long meetingId, @Param("userId") Long userId);
}
