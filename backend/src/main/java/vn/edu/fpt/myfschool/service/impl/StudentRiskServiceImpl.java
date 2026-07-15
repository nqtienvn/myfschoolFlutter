package vn.edu.fpt.myfschool.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.*;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.StudentRiskService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentRiskServiceImpl implements StudentRiskService {
    private final StudentRiskConfigRepository configs;
    private final StudentRiskFlagRepository flags;
    private final AcademicYearRepository years;
    private final SemesterRepository semesters;
    private final ClassRepository classes;
    private final EnrollmentRepository enrollments;
    private final SemesterResultRepository semesterResults;
    private final AttendanceRepository attendance;
    private final StudentPeriodicReportRepository periodicReports;
    private final TuitionBillRepository tuitionBills;
    private final TeacherRepository teachers;
    private final HomeroomAssignmentRepository homeroomAssignments;
    private final UserRepository users;
    private final ObjectMapper objectMapper;

    @Override
    public StudentRiskConfigDto getConfig(Long academicYearId) {
        return toConfig(configForYear(requireYear(academicYearId)));
    }

    @Override
    public StudentRiskConfigDto updateConfig(UpdateStudentRiskConfigRequest request) {
        AcademicYear year = requireYear(request.academicYearId());
        StudentRiskConfig config = configForYear(year);
        config.setMinGpa(request.minGpa());
        config.setMinAttendanceRate(request.minAttendanceRate());
        config.setMaxUnexcusedAbsences(request.maxUnexcusedAbsences());
        config.setConductRiskValues(trim(request.conductRiskValues()));
        config.setIncludeOverdueTuition(request.includeOverdueTuition());
        config.setOverdueTuitionDays(request.overdueTuitionDays());
        config.setGpaSeverity(request.gpaSeverity());
        config.setAttendanceSeverity(request.attendanceSeverity());
        config.setAbsenceSeverity(request.absenceSeverity());
        config.setConductSeverity(request.conductSeverity());
        config.setTuitionSeverity(request.tuitionSeverity());
        return toConfig(configs.save(config));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentRiskFlagDto> getRisks(Long academicYearId, Long semesterId, Long classId,
            RiskStatus status, Long requesterId, UserRole requesterRole) {
        requireScope(academicYearId, semesterId, classId);
        authorizeClass(academicYearId, classId, requesterId, requesterRole);
        List<StudentRiskFlag> result = status == null
                ? flags.findByAcademicYearIdAndSemesterIdAndClsIdOrderBySeverityDescDetectedAtDesc(
                        academicYearId, semesterId, classId)
                : flags.findByAcademicYearIdAndSemesterIdAndClsIdAndStatusOrderBySeverityDescDetectedAtDesc(
                        academicYearId, semesterId, classId, status);
        return result.stream().map(this::toFlag).toList();
    }

    @Override
    public StudentRiskFlagDto acknowledge(Long id, Long requesterId, UserRole requesterRole) {
        StudentRiskFlag flag = requireFlag(id);
        authorizeClass(flag.getAcademicYear().getId(), flag.getCls().getId(), requesterId, requesterRole);
        if (flag.getStatus() != RiskStatus.RESOLVED) {
            flag.setStatus(RiskStatus.ACKNOWLEDGED);
            flag.setAcknowledgedBy(requireUser(requesterId));
        }
        return toFlag(flags.save(flag));
    }

    @Override
    public StudentRiskFlagDto resolve(Long id, Long requesterId, UserRole requesterRole) {
        StudentRiskFlag flag = requireFlag(id);
        authorizeClass(flag.getAcademicYear().getId(), flag.getCls().getId(), requesterId, requesterRole);
        flag.setStatus(RiskStatus.RESOLVED);
        flag.setResolvedBy(requireUser(requesterId));
        flag.setResolvedAt(LocalDateTime.now());
        return toFlag(flags.save(flag));
    }

    @Override
    public List<StudentRiskFlagDto> recalculateClass(Long academicYearId, Long semesterId, Long classId) {
        Scope scope = requireScope(academicYearId, semesterId, classId);
        StudentRiskConfig config = configForYear(scope.year());
        List<Student> roster = enrollments.findActiveStudentsByClassAndYear(classId, academicYearId);
        Map<Long, SemesterResult> results = semesterResults.findByClassIdAndSemesterIdOrderByRankAsc(classId, semesterId)
                .stream().collect(Collectors.toMap(item -> item.getStudent().getId(), Function.identity(), (a, b) -> a));
        Map<Long, List<Attendance>> attendanceByStudent = attendance
                .findByClsIdAndDateBetween(classId, scope.semester().getStartDate(), scope.semester().getEndDate())
                .stream().collect(Collectors.groupingBy(item -> item.getStudent().getId()));
        Map<Long, StudentPeriodicReport> reports = periodicReports
                .findByAcademicYearIdAndSemesterId(academicYearId, semesterId).stream()
                .filter(item -> item.getCls().getId().equals(classId))
                .collect(Collectors.toMap(item -> item.getStudent().getId(), Function.identity(), (a, b) -> a));
        Map<Long, List<TuitionBill>> overdueByStudent = tuitionBills
                .findByClassIdAndSemesterIdAndStatus(classId, semesterId, BillStatus.UNPAID).stream()
                .filter(item -> item.getDueDate().plusDays(config.getOverdueTuitionDays()).isBefore(LocalDate.now()))
                .collect(Collectors.groupingBy(item -> item.getStudent().getId()));

        Set<String> activeKeys = new HashSet<>();
        for (Student student : roster) {
            Map<RiskType, Candidate> candidates = evaluate(student, config, results.get(student.getId()),
                    attendanceByStudent.getOrDefault(student.getId(), List.of()), reports.get(student.getId()),
                    overdueByStudent.getOrDefault(student.getId(), List.of()));
            for (Candidate candidate : candidates.values()) {
                activeKeys.add(key(student.getId(), candidate.type()));
                upsert(scope, student, candidate);
            }
        }

        List<StudentRiskFlag> existing = flags
                .findByAcademicYearIdAndSemesterIdAndClsIdOrderBySeverityDescDetectedAtDesc(
                        academicYearId, semesterId, classId);
        for (StudentRiskFlag flag : existing) {
            if (!activeKeys.contains(key(flag.getStudent().getId(), flag.getRiskType()))
                    && flag.getStatus() != RiskStatus.RESOLVED) {
                flag.setStatus(RiskStatus.RESOLVED);
                flag.setResolvedAt(LocalDateTime.now());
                flag.setResolvedBy(null);
                flags.save(flag);
            }
        }
        return flags.findByAcademicYearIdAndSemesterIdAndClsIdOrderBySeverityDescDetectedAtDesc(
                academicYearId, semesterId, classId).stream().map(this::toFlag).toList();
    }

    @Override
    public void recalculateForDate(Long academicYearId, Long classId, LocalDate date) {
        semesters.findByAcademicYearId(academicYearId).stream()
                .filter(item -> !date.isBefore(item.getStartDate()) && !date.isAfter(item.getEndDate()))
                .findFirst().ifPresent(item -> recalculateClass(academicYearId, item.getId(), classId));
    }

    private Map<RiskType, Candidate> evaluate(Student student, StudentRiskConfig config, SemesterResult result,
            List<Attendance> attendanceRows, StudentPeriodicReport report, List<TuitionBill> overdueBills) {
        Map<RiskType, Candidate> values = new EnumMap<>(RiskType.class);
        if (config.getMinGpa() != null && result != null && result.getGpa() != null
                && result.getGpa().compareTo(config.getMinGpa()) < 0) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("gpa", result.getGpa());
            if (result.getRank() != null) snapshot.put("rank", result.getRank());
            values.put(RiskType.LOW_GPA, candidate(RiskType.LOW_GPA, config.getGpaSeverity(),
                    result.getGpa().toPlainString(), config.getMinGpa().toPlainString(),
                    "GPA thấp hơn ngưỡng theo dõi", snapshot));
        }
        if (!attendanceRows.isEmpty()) {
            long present = attendanceRows.stream().filter(item -> item.getStatus() == AttendanceStatus.PRESENT).count();
            long unexcused = attendanceRows.stream()
                    .filter(item -> item.getStatus() == AttendanceStatus.ABSENT_WITHOUT_LEAVE).count();
            BigDecimal rate = BigDecimal.valueOf(present * 100.0 / attendanceRows.size()).setScale(2, RoundingMode.HALF_UP);
            if (config.getMinAttendanceRate() != null && rate.compareTo(config.getMinAttendanceRate()) < 0) {
                values.put(RiskType.LOW_ATTENDANCE, candidate(RiskType.LOW_ATTENDANCE,
                        config.getAttendanceSeverity(), rate.toPlainString(), config.getMinAttendanceRate().toPlainString(),
                        "Tỷ lệ chuyên cần thấp hơn ngưỡng", Map.of("present", present, "total", attendanceRows.size(), "rate", rate)));
            }
            if (config.getMaxUnexcusedAbsences() != null && unexcused > config.getMaxUnexcusedAbsences()) {
                values.put(RiskType.UNEXCUSED_ABSENCE, candidate(RiskType.UNEXCUSED_ABSENCE,
                        config.getAbsenceSeverity(), String.valueOf(unexcused), String.valueOf(config.getMaxUnexcusedAbsences()),
                        "Số buổi vắng không phép vượt ngưỡng", Map.of("unexcusedAbsences", unexcused)));
            }
        }
        Set<String> conductRisks = splitValues(config.getConductRiskValues());
        if (report != null && report.getStatus() == PeriodicReportStatus.PUBLISHED
                && report.getConduct() != null && conductRisks.contains(report.getConduct().toLowerCase(Locale.ROOT))) {
            values.put(RiskType.CONDUCT, candidate(RiskType.CONDUCT, config.getConductSeverity(), report.getConduct(),
                    config.getConductRiskValues(), "Hạnh kiểm thuộc nhóm cần theo dõi",
                    Map.of("conduct", report.getConduct(), "reportId", report.getId())));
        }
        if (Boolean.TRUE.equals(config.getIncludeOverdueTuition()) && !overdueBills.isEmpty()) {
            BigDecimal amount = overdueBills.stream().map(TuitionBill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            values.put(RiskType.OVERDUE_TUITION, candidate(RiskType.OVERDUE_TUITION, config.getTuitionSeverity(),
                    amount.toPlainString(), config.getOverdueTuitionDays() + " ngày",
                    "Có học phí quá hạn", Map.of("billCount", overdueBills.size(), "amount", amount)));
        }
        return values;
    }

    private void upsert(Scope scope, Student student, Candidate candidate) {
        StudentRiskFlag flag = flags.findByAcademicYearIdAndSemesterIdAndClsIdAndStudentIdAndRiskType(
                scope.year().getId(), scope.semester().getId(), scope.cls().getId(), student.getId(), candidate.type())
                .orElseGet(StudentRiskFlag::new);
        flag.setAcademicYear(scope.year());
        flag.setSemester(scope.semester());
        flag.setCls(scope.cls());
        flag.setStudent(student);
        flag.setRiskType(candidate.type());
        flag.setSeverity(candidate.severity());
        flag.setMetricValue(candidate.metric());
        flag.setThresholdValue(candidate.threshold());
        flag.setMessage(candidate.message());
        flag.setSourceSnapshotJson(json(candidate.snapshot()));
        if (flag.getId() == null || flag.getStatus() == RiskStatus.RESOLVED) {
            flag.setStatus(RiskStatus.OPEN);
            flag.setDetectedAt(LocalDateTime.now());
            flag.setAcknowledgedBy(null);
            flag.setResolvedBy(null);
            flag.setResolvedAt(null);
        }
        flags.save(flag);
    }

    private Candidate candidate(RiskType type, RiskSeverity severity, String metric, String threshold,
            String message, Map<String, Object> snapshot) {
        return new Candidate(type, severity, metric, threshold, message, snapshot);
    }

    private StudentRiskConfig configForYear(AcademicYear year) {
        return configs.findByAcademicYearId(year.getId()).orElseGet(() -> {
            StudentRiskConfig config = new StudentRiskConfig();
            config.setAcademicYear(year);
            config.setMinGpa(BigDecimal.valueOf(5));
            config.setMinAttendanceRate(BigDecimal.valueOf(80));
            config.setMaxUnexcusedAbsences(3);
            config.setConductRiskValues("Yếu");
            return configs.save(config);
        });
    }

    private Scope requireScope(Long academicYearId, Long semesterId, Long classId) {
        AcademicYear year = requireYear(academicYearId);
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        SchoolClass cls = classes.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        if (!semester.getAcademicYear().getId().equals(academicYearId)
                || !cls.getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Năm học, học kỳ và lớp không cùng phạm vi");
        }
        return new Scope(year, semester, cls);
    }

    private void authorizeClass(Long academicYearId, Long classId, Long requesterId, UserRole role) {
        if (role == UserRole.ADMIN) return;
        if (role != UserRole.TEACHER) throw new ForbiddenException("Cảnh báo chỉ dành cho GVCN và Admin");
        Teacher teacher = teachers.findByUserId(requesterId)
                .orElseThrow(() -> new ForbiddenException("Không tìm thấy hồ sơ giáo viên"));
        if (homeroomAssignments.findActiveByClassAndYear(classId, academicYearId)
                .filter(item -> item.getTeacher().getId().equals(teacher.getId())).isEmpty()) {
            throw new ForbiddenException("Giáo viên chỉ được xem cảnh báo lớp chủ nhiệm");
        }
    }

    private AcademicYear requireYear(Long id) {
        return years.findById(id).orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }

    private User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private StudentRiskFlag requireFlag(Long id) {
        return flags.findById(id).orElseThrow(() -> new ResourceNotFoundException("StudentRiskFlag", "id", id));
    }

    private StudentRiskConfigDto toConfig(StudentRiskConfig value) {
        return new StudentRiskConfigDto(value.getId(), value.getAcademicYear().getId(), value.getMinGpa(),
                value.getMinAttendanceRate(), value.getMaxUnexcusedAbsences(), value.getConductRiskValues(),
                value.getIncludeOverdueTuition(), value.getOverdueTuitionDays(), value.getGpaSeverity(),
                value.getAttendanceSeverity(), value.getAbsenceSeverity(), value.getConductSeverity(),
                value.getTuitionSeverity());
    }

    private StudentRiskFlagDto toFlag(StudentRiskFlag value) {
        return new StudentRiskFlagDto(value.getId(), value.getAcademicYear().getId(), value.getSemester().getId(),
                value.getCls().getId(), value.getCls().getName(), value.getStudent().getId(),
                value.getStudent().getUser().getName(), value.getStudent().getStudentCode(), value.getRiskType(),
                value.getSeverity(), value.getMetricValue(), value.getThresholdValue(), value.getMessage(),
                value.getStatus(), value.getDetectedAt(),
                value.getAcknowledgedBy() == null ? null : value.getAcknowledgedBy().getId(),
                value.getResolvedBy() == null ? null : value.getResolvedBy().getId(), value.getResolvedAt(),
                value.getSourceSnapshotJson());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể tạo snapshot cảnh báo", exception);
        }
    }

    private Set<String> splitValues(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isEmpty())
                .map(item -> item.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    private String trim(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String key(Long studentId, RiskType type) {
        return studentId + ":" + type;
    }

    private record Scope(AcademicYear year, Semester semester, SchoolClass cls) {}
    private record Candidate(RiskType type, RiskSeverity severity, String metric, String threshold,
                             String message, Map<String, Object> snapshot) {}
}
