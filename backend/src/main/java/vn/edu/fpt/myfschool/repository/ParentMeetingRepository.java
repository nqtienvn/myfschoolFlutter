package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.ParentMeeting;

import java.util.List;

public interface ParentMeetingRepository extends JpaRepository<ParentMeeting, Long> {
    List<ParentMeeting> findByAcademicYearIdAndSemesterIdAndClsIdOrderByStartsAtDesc(
            Long academicYearId, Long semesterId, Long classId);
    List<ParentMeeting> findByClsIdAndSemesterId(Long classId, Long semesterId);
}
