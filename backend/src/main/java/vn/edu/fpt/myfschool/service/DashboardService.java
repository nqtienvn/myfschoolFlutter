package vn.edu.fpt.myfschool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

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

    public DashboardStudentStatsDto getStudentDashboard(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", studentUserId));
        Semester sem = semesterRepository.findByIsCurrentTrue().orElse(null);
        if (sem == null) return null;

        long totalDays = attendanceRepository.countByStudentIdAndStatusAndDateBetween(
            student.getId(), "PRESENT", sem.getStartDate(), sem.getEndDate());
        long lateDays = attendanceRepository.countByStudentIdAndStatusAndDateBetween(
            student.getId(), "LATE", sem.getStartDate(), sem.getEndDate());
        double rate = (totalDays + lateDays) > 0 ? (double)(totalDays) / (totalDays + lateDays) * 100 : 100;

        SemesterResult sr = semesterResultRepository.findByStudentIdAndSemesterId(student.getId(), sem.getId()).orElse(null);
        return new DashboardStudentStatsDto(student.getId(), student.getUser().getName(),
            student.getStudentCode(), student.getCurrentClass() != null ? student.getCurrentClass().getName() : "",
            rate, (int)totalDays, 0, (int)lateDays,
            sr != null ? sr.getGpa() : null, sr != null ? sr.getAcademicAbility() : null,
            sr != null ? sr.getConduct() : null, sr != null ? sr.getRank() : null);
    }
}
