package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.DashboardStudentStatsDto;
import vn.edu.fpt.myfschool.common.dto.DashboardTeacherStatsDto;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.AnnouncementDeliveryStatus;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.Announcement;
import vn.edu.fpt.myfschool.entity.Attendance;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.SemesterResult;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.StudentGuardian;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.AnnouncementClassRepository;
import vn.edu.fpt.myfschool.repository.AnnouncementReadRepository;
import vn.edu.fpt.myfschool.repository.AnnouncementRepository;
import vn.edu.fpt.myfschool.repository.AttendanceRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.repository.HomeroomAssignmentRepository;
import vn.edu.fpt.myfschool.repository.ParentRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.repository.SemesterResultRepository;
import vn.edu.fpt.myfschool.repository.StudentGuardianRepository;
import vn.edu.fpt.myfschool.repository.StudentRepository;
import vn.edu.fpt.myfschool.repository.TeacherRepository;
import vn.edu.fpt.myfschool.service.DashboardService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service("dashboardService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final AttendanceRepository attendanceRepository;
    private final SemesterResultRepository semesterResultRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementClassRepository announcementClassRepository;
    private final AnnouncementReadRepository announcementReadRepository;

    @Override
    public DashboardStudentStatsDto getStudentDashboard(
            Long requestUserId, Long requestedStudentId,
            Long academicYearId, Long semesterId) {
        Student student = resolveAccessibleStudent(requestUserId, requestedStudentId);
        Semester semester = resolveSemester(academicYearId, semesterId);
        Long resolvedYearId = semester.getAcademicYear().getId();
        Enrollment enrollment = enrollmentRepository
            .findFirstByStudentIdAndAcademicYearIdOrderByIdDesc(student.getId(), resolvedYearId)
            .orElseThrow(() -> new BadRequestException(
                "Học sinh không thuộc năm học đã chọn"));

        long presentSessions = attendanceRepository.countByStudentIdAndStatusAndDateBetween(
            student.getId(), AttendanceStatus.PRESENT,
            semester.getStartDate(), semester.getEndDate());
        long absentWithLeave = attendanceRepository.countByStudentIdAndStatusAndDateBetween(
            student.getId(), AttendanceStatus.ABSENT_WITH_LEAVE,
            semester.getStartDate(), semester.getEndDate());
        long absentWithoutLeave = attendanceRepository.countByStudentIdAndStatusAndDateBetween(
            student.getId(), AttendanceStatus.ABSENT_WITHOUT_LEAVE,
            semester.getStartDate(), semester.getEndDate());

        long absentSessions = absentWithLeave + absentWithoutLeave;
        double attendanceRate = percentage(
            presentSessions, presentSessions + absentSessions);
        SemesterResult result = semesterResultRepository
            .findByStudentIdAndSemesterId(student.getId(), semester.getId())
            .orElse(null);
        HomeroomAssignment homeroom = findHomeroomForClass(
            enrollment.getCls().getId(), resolvedYearId, semester);

        return new DashboardStudentStatsDto(
            student.getId(), student.getUser().getName(), student.getStudentCode(),
            enrollment.getCls().getId(), enrollment.getCls().getName(),
            enrollment.getCls().getSchoolName(), resolvedYearId,
            semester.getAcademicYear().getName(), semester.getId(), semester.getName(),
            attendanceRate, (int) presentSessions, (int) absentSessions,
            result != null ? result.getGpa() : null,
            result != null ? result.getAcademicAbility() : null,
            result != null ? result.getConduct() : null,
            result != null ? result.getRank() : null,
            homeroom != null ? homeroom.getTeacher().getUser().getName() : null,
            homeroom != null ? homeroom.getTeacher().getUser().getPhone() : null);
    }

    private Student resolveAccessibleStudent(Long requestUserId, Long requestedStudentId) {
        var self = studentRepository.findByUserId(requestUserId);
        if (self.isPresent()) {
            if (requestedStudentId != null
                    && !requestedStudentId.equals(self.get().getId())) {
                throw new ForbiddenException(
                    "Học sinh chỉ được xem dashboard của chính mình");
            }
            return self.get();
        }

        Parent parent = parentRepository.findByUserId(requestUserId)
            .orElseThrow(() -> new ForbiddenException(
                "Tài khoản không có quyền xem dashboard học sinh"));
        if (requestedStudentId == null
                || !studentGuardianRepository.existsByStudentIdAndGuardianId(
                    requestedStudentId, parent.getId())) {
            throw new ForbiddenException("Phụ huynh không quản lý học sinh này");
        }
        return studentRepository.findById(requestedStudentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Student", "id", requestedStudentId));
    }

    @Override
    public DashboardTeacherStatsDto getTeacherDashboard(
            Long teacherUserId, Long academicYearId, Long semesterId) {
        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", teacherUserId));
        Semester semester = resolveSemester(academicYearId, semesterId);
        Long resolvedYearId = semester.getAcademicYear().getId();
        LocalDate referenceDate = selectedSemesterReferenceDate(semester);
        HomeroomAssignment homeroom = homeroomAssignmentRepository
            .findByTeacherIdAndAcademicYearId(teacher.getId(), resolvedYearId).stream()
            .filter(assignment -> isEffectiveOn(assignment, referenceDate))
            .max(Comparator.comparing(HomeroomAssignment::getEffectiveFrom))
            .orElseThrow(() -> new ForbiddenException(
                "Giáo viên không được phân công chủ nhiệm tại ngày tham chiếu của học kỳ đã chọn"));
        SchoolClass schoolClass = homeroom.getCls();

        List<Attendance> attendance = attendanceRepository.findByClsIdAndDateBetween(
            schoolClass.getId(), semester.getStartDate(), semester.getEndDate());
        long presentSessions = attendance.stream()
            .filter(item -> item.getStatus() == AttendanceStatus.PRESENT)
            .count();
        Double attendanceRate = attendance.isEmpty()
            ? null : percentage(presentSessions, attendance.size());

        List<BigDecimal> gpas = semesterResultRepository
            .findByClassIdAndSemesterIdOrderByRankAsc(
                schoolClass.getId(), semester.getId()).stream()
            .map(SemesterResult::getGpa)
            .filter(Objects::nonNull)
            .toList();
        BigDecimal averageGpa = gpas.isEmpty() ? null : gpas.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(gpas.size()), 2, RoundingMode.HALF_UP);

        return new DashboardTeacherStatsDto(
            schoolClass.getId(), schoolClass.getName(),
            resolvedYearId, semester.getAcademicYear().getName(),
            semester.getId(), semester.getName(),
            attendanceRate, averageGpa,
            calculateParentReadRate(teacher, schoolClass, semester));
    }

    private Semester resolveSemester(Long academicYearId, Long semesterId) {
        Semester semester = semesterId != null
            ? semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId))
            : semesterRepository
                .findFirstByIsCurrentTrueAndAcademicYearStatus(AcademicYearStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "current", true));
        if (academicYearId != null
                && !academicYearId.equals(semester.getAcademicYear().getId())) {
            throw new BadRequestException("Học kỳ không thuộc năm học đã chọn");
        }
        return semester;
    }

    private HomeroomAssignment findHomeroomForClass(
            Long classId, Long academicYearId, Semester semester) {
        LocalDate referenceDate = selectedSemesterReferenceDate(semester);
        return homeroomAssignmentRepository
            .findByClsIdAndAcademicYearId(classId, academicYearId).stream()
            .filter(assignment -> isEffectiveOn(assignment, referenceDate))
            .max(Comparator.comparing(HomeroomAssignment::getEffectiveFrom))
            .orElse(null);
    }

    private LocalDate selectedSemesterReferenceDate(Semester semester) {
        LocalDate today = LocalDate.now();
        if (!today.isBefore(semester.getStartDate())
                && !today.isAfter(semester.getEndDate())) {
            return today;
        }
        return semester.getStartDate();
    }

    private boolean isEffectiveOn(HomeroomAssignment assignment, LocalDate date) {
        return !assignment.getEffectiveFrom().isAfter(date)
            && (assignment.getEffectiveTo() == null
                || !assignment.getEffectiveTo().isBefore(date));
    }

    private Double calculateParentReadRate(
            Teacher teacher, SchoolClass schoolClass, Semester semester) {
        Long academicYearId = semester.getAcademicYear().getId();
        Set<Long> guardianUserIds = enrollmentRepository
            .findActiveStudentsByClassAndYear(schoolClass.getId(), academicYearId).stream()
            .flatMap(student -> studentGuardianRepository
                .findByStudentId(student.getId()).stream())
            .map(StudentGuardian::getGuardian)
            .map(Parent::getUser)
            .filter(Objects::nonNull)
            .map(User::getId)
            .collect(Collectors.toSet());
        if (guardianUserIds.isEmpty()) return null;

        List<Long> announcementIds = announcementRepository
            .findByTeacherIdOrderByCreatedAtDesc(teacher.getId()).stream()
            .filter(item -> item.getAcademicYear().getId().equals(academicYearId))
            .filter(item -> isCreatedWithin(item, semester))
            .filter(item -> item.getDeliveryStatus() == AnnouncementDeliveryStatus.PUBLISHED)
            .filter(item -> item.getTargetRole() == TargetRole.PARENT
                || item.getTargetRole() == TargetRole.ALL)
            .filter(item -> announcementClassRepository.findByAnnouncementId(item.getId()).stream()
                .anyMatch(link -> link.getCls().getId().equals(schoolClass.getId())))
            .map(Announcement::getId)
            .toList();
        if (announcementIds.isEmpty()) return null;

        long reads = announcementReadRepository
            .countByAnnouncementIdInAndUserIdInAndReadAtIsNotNull(
                announcementIds, guardianUserIds);
        long potentialReads = (long) announcementIds.size() * guardianUserIds.size();
        return percentage(reads, potentialReads);
    }

    private boolean isCreatedWithin(Announcement announcement, Semester semester) {
        LocalDateTime createdAt = announcement.getCreatedAt();
        if (createdAt == null) return false;
        LocalDate createdDate = createdAt.toLocalDate();
        return !createdDate.isBefore(semester.getStartDate())
            && !createdDate.isAfter(semester.getEndDate());
    }

    private double percentage(long numerator, long denominator) {
        if (denominator == 0) return 0;
        return Math.round((double) numerator / denominator * 1000.0) / 10.0;
    }
}
