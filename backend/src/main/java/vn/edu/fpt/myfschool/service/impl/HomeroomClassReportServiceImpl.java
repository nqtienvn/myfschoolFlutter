package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.ClassSummaryDto;
import vn.edu.fpt.myfschool.common.enums.*;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.HomeroomClassReportService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeroomClassReportServiceImpl implements HomeroomClassReportService {
    private final AcademicYearRepository years;
    private final SemesterRepository semesters;
    private final ClassRepository classes;
    private final TeacherRepository teachers;
    private final HomeroomAssignmentRepository homeroomAssignments;
    private final EnrollmentRepository enrollments;
    private final AttendanceRepository attendance;
    private final SemesterResultRepository semesterResults;
    private final StudentRiskFlagRepository risks;
    private final TeachingAssignmentRepository assignments;
    private final SubjectStudentReviewRepository reviews;
    private final ParentContactLogRepository contacts;
    private final ParentMeetingRepository meetings;
    private final StudentEventRepository events;

    @Override
    public List<ClassSummaryDto> getSummaries(Long academicYearId, Long semesterId, Long classId,
            Integer gradeLevel, Long requesterId, UserRole requesterRole) {
        AcademicYear year = years.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", academicYearId));
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!semester.getAcademicYear().getId().equals(year.getId())) {
            throw new ForbiddenException("Học kỳ không thuộc năm học đã chọn");
        }
        List<SchoolClass> selected = classId == null ? classes.findByAcademicYearId(academicYearId)
                : List.of(requireClass(classId, academicYearId));
        if (gradeLevel != null) selected = selected.stream()
                .filter(item -> item.getGradeLevel().equals(gradeLevel)).toList();
        if (requesterRole == UserRole.TEACHER) {
            Teacher teacher = teachers.findByUserId(requesterId)
                    .orElseThrow(() -> new ForbiddenException("Không tìm thấy hồ sơ giáo viên"));
            Set<Long> allowed = homeroomAssignments.findActiveByTeacherAndYear(teacher.getId(), academicYearId)
                    .stream().map(item -> item.getCls().getId()).collect(Collectors.toSet());
            selected = selected.stream().filter(item -> allowed.contains(item.getId())).toList();
            if (classId != null && selected.isEmpty()) {
                throw new ForbiddenException("Giáo viên chỉ được xem báo cáo lớp chủ nhiệm");
            }
        } else if (requesterRole != UserRole.ADMIN) {
            throw new ForbiddenException("Báo cáo lớp chỉ dành cho GVCN và Admin");
        }
        return selected.stream().sorted(Comparator.comparing(SchoolClass::getName))
                .map(cls -> build(year, semester, cls)).toList();
    }

    private ClassSummaryDto build(AcademicYear year, Semester semester, SchoolClass cls) {
        List<Student> roster = enrollments.findActiveStudentsByClassAndYear(cls.getId(), year.getId());
        List<Attendance> attendanceRows = attendance.findByClsIdAndDateBetween(
                cls.getId(), semester.getStartDate(), semester.getEndDate());
        long present = attendanceRows.stream().filter(item -> item.getStatus() == AttendanceStatus.PRESENT).count();
        BigDecimal attendanceRate = percent(present, attendanceRows.size());

        List<SemesterResult> results = semesterResults.findByClassIdAndSemesterIdOrderByRankAsc(
                cls.getId(), semester.getId());
        BigDecimal averageGpa = results.stream().map(SemesterResult::getGpa).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long gpaCount = results.stream().filter(item -> item.getGpa() != null).count();
        averageGpa = gpaCount == 0 ? BigDecimal.ZERO
                : averageGpa.divide(BigDecimal.valueOf(gpaCount), 2, RoundingMode.HALF_UP);
        Map<String, Long> ability = distribution(results.stream().map(SemesterResult::getAcademicAbility).toList());
        Map<String, Long> conduct = distribution(results.stream().map(SemesterResult::getConduct).toList());

        long subjectCount = assignments.findByClsIdAndStatus(cls.getId(), AssignmentStatus.ACTIVE).stream()
                .filter(item -> !item.getEffectiveFrom().isAfter(semester.getEndDate())
                        && (item.getEffectiveTo() == null || !item.getEffectiveTo().isBefore(semester.getStartDate())))
                .map(item -> item.getSubject().getId()).distinct().count();
        long expectedReviews = subjectCount * roster.size();
        long submittedReviews = reviews.findByClsIdAndSemesterId(cls.getId(), semester.getId()).stream()
                .filter(item -> item.getStatus() == SubjectReviewStatus.SUBMITTED).count();

        List<StudentRiskFlag> riskRows = risks
                .findByAcademicYearIdAndSemesterIdAndClsIdOrderBySeverityDescDetectedAtDesc(
                        year.getId(), semester.getId(), cls.getId());
        long openRiskCount = riskRows.stream().filter(item -> item.getStatus() != RiskStatus.RESOLVED)
                .map(item -> item.getStudent().getId()).distinct().count();
        long contactCount = contacts.findByClsIdAndSemesterId(cls.getId(), semester.getId()).size();
        List<ParentMeeting> meetingRows = meetings.findByClsIdAndSemesterId(cls.getId(), semester.getId());
        long participantCount = meetingRows.stream().mapToLong(item -> item.getParticipants().size()).sum();
        long attendedCount = meetingRows.stream().flatMap(item -> item.getParticipants().stream())
                .filter(item -> item.getAttendance() == MeetingAttendance.ATTENDED).count();
        List<StudentEvent> eventRows = events.findByClsIdAndSemesterId(cls.getId(), semester.getId()).stream()
                .filter(item -> item.getStatus() == StudentEventStatus.SUBMITTED).toList();

        return new ClassSummaryDto(year.getId(), semester.getId(), cls.getId(), cls.getName(), cls.getGradeLevel(),
                roster.size(), attendanceRate, openRiskCount, averageGpa, ability, conduct, submittedReviews,
                expectedReviews, percent(submittedReviews, expectedReviews), contactCount, meetingRows.size(),
                percent(attendedCount, participantCount),
                eventRows.stream().filter(item -> item.getEventType() == StudentEventType.REWARD).count(),
                eventRows.stream().filter(item -> item.getEventType() == StudentEventType.VIOLATION).count());
    }

    private Map<String, Long> distribution(List<String> values) {
        return values.stream().filter(Objects::nonNull).collect(Collectors.groupingBy(
                value -> value, LinkedHashMap::new, Collectors.counting()));
    }

    private BigDecimal percent(long part, long total) {
        if (total == 0) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(part * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private SchoolClass requireClass(Long id, Long academicYearId) {
        SchoolClass cls = classes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
        if (!cls.getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Lớp không thuộc năm học đã chọn");
        }
        return cls;
    }
}
