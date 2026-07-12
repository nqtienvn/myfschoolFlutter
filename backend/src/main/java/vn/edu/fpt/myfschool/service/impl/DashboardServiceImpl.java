package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.SemesterResult;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.repository.*;

@Service("dashboardService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final AttendanceRepository attendanceRepository;
    private final SemesterResultRepository semesterResultRepository;

    @Override
    public DashboardStudentStatsDto getStudentDashboard(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", studentUserId));
        Semester sem = semesterRepository
            .findFirstByIsCurrentTrueAndAcademicYearStatus(AcademicYearStatus.ACTIVE)
            .orElse(null);
        if (sem == null) return null;

        long presentSessions = attendanceRepository.countByStudentIdAndStatusAndDateBetween(
            student.getId(), AttendanceStatus.PRESENT, sem.getStartDate(), sem.getEndDate());
        long absentWithLeave = attendanceRepository.countByStudentIdAndStatusAndDateBetween(
            student.getId(), AttendanceStatus.ABSENT_WITH_LEAVE, sem.getStartDate(), sem.getEndDate());
        long absentWithoutLeave = attendanceRepository.countByStudentIdAndStatusAndDateBetween(
            student.getId(), AttendanceStatus.ABSENT_WITHOUT_LEAVE, sem.getStartDate(), sem.getEndDate());

        long totalSessions = presentSessions + absentWithLeave + absentWithoutLeave;
        long attendedSessions = presentSessions;
        long absentSessions = absentWithLeave + absentWithoutLeave;
        double rate = totalSessions > 0 ? (double) attendedSessions / totalSessions * 100 : 0;
        rate = Math.round(rate * 10.0) / 10.0;

        SemesterResult sr = semesterResultRepository.findByStudentIdAndSemesterId(student.getId(), sem.getId()).orElse(null);
        return new DashboardStudentStatsDto(student.getId(), student.getUser().getName(),
            student.getStudentCode(), student.getCurrentClass() != null ? student.getCurrentClass().getName() : "",
            rate, (int) presentSessions, (int) absentSessions,
            sr != null ? sr.getGpa() : null, sr != null ? sr.getAcademicAbility() : null,
            sr != null ? sr.getConduct() : null, sr != null ? sr.getRank() : null);
    }
}
