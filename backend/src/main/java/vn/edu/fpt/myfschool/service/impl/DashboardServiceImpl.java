package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;

@Service("dashboardService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ConversationService conversationService;
    private final NotificationService notificationService;
    private final AttendanceRepository attendanceRepository;
    private final GradeRepository gradeRepository;
    private final SemesterResultRepository semesterResultRepository;
    private final ClassRepository classRepository;

    @Override
    public DashboardStudentStatsDto getStudentDashboard(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", studentUserId));
        Semester sem = semesterRepository.findByIsCurrentTrue().orElse(null);
        if (sem == null) return null;

        long presentDays = attendanceRepository.countByStudentIdAndStatusAndDateBetween(
            student.getId(), AttendanceStatus.PRESENT, sem.getStartDate(), sem.getEndDate());
        long lateDays = attendanceRepository.countByStudentIdAndStatusAndDateBetween(
            student.getId(), AttendanceStatus.LATE, sem.getStartDate(), sem.getEndDate());
        long absentWithLeave = attendanceRepository.countByStudentIdAndStatusAndDateBetween(
            student.getId(), AttendanceStatus.ABSENT_WITH_LEAVE, sem.getStartDate(), sem.getEndDate());
        long absentWithoutLeave = attendanceRepository.countByStudentIdAndStatusAndDateBetween(
            student.getId(), AttendanceStatus.ABSENT_WITHOUT_LEAVE, sem.getStartDate(), sem.getEndDate());

        long totalSessions = presentDays + lateDays + absentWithLeave + absentWithoutLeave;
        long attendedSessions = presentDays + lateDays;
        long absentDays = absentWithLeave + absentWithoutLeave;
        double rate = totalSessions > 0 ? (double) attendedSessions / totalSessions * 100 : 0;
        rate = Math.round(rate * 10.0) / 10.0;

        SemesterResult sr = semesterResultRepository.findByStudentIdAndSemesterId(student.getId(), sem.getId()).orElse(null);
        return new DashboardStudentStatsDto(student.getId(), student.getUser().getName(),
            student.getStudentCode(), student.getCurrentClass() != null ? student.getCurrentClass().getName() : "",
            rate, (int) presentDays, (int) absentDays, (int) lateDays,
            sr != null ? sr.getGpa() : null, sr != null ? sr.getAcademicAbility() : null,
            sr != null ? sr.getConduct() : null, sr != null ? sr.getRank() : null);
    }
}
