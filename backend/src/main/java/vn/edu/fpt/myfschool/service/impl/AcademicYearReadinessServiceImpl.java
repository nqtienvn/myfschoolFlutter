package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.AcademicYearReadinessService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicYearReadinessServiceImpl implements AcademicYearReadinessService {
    private final AcademicYearRepository yearRepository;
    private final ClassRepository classRepository;
    private final SemesterRepository semesterRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final HomeroomAssignmentRepository homeroomRepository;
    private final TeachingAssignmentRepository teachingRepository;
    private final AcademicYearSubjectRepository yearSubjectRepository;
    private final AcademicYearShiftRepository yearShiftRepository;
    private final AcademicYearPeriodRepository yearPeriodRepository;

    @Override
    public AcademicYearReadinessDto check(Long yearId) {
        yearRepository.findById(yearId).orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", yearId));
        List<ReadinessCheckDto> checks = new ArrayList<>();
        boolean masterReady = !yearSubjectRepository.findByAcademicYearId(yearId).isEmpty()
            && !yearShiftRepository.findByAcademicYearId(yearId).isEmpty()
            && !yearPeriodRepository.findByAcademicYearId(yearId).isEmpty();
        checks.add(check("MASTER_DATA", "Danh mục áp dụng", masterReady, masterReady ? "Đã chọn môn, ca và tiết học." : "Phải chọn ít nhất một môn, ca và tiết học."));

        List<SchoolClass> classes = classRepository.findByAcademicYearId(yearId);
        checks.add(check("CLASSES", "Danh sách lớp", !classes.isEmpty(), classes.isEmpty() ? "Chưa có lớp học." : "Có " + classes.size() + " lớp học."));

        long missingHomeroom = classes.stream().filter(cls -> homeroomRepository.findActiveByClassAndYear(cls.getId(), yearId).isEmpty()).count();
        checks.add(check("HOMEROOM", "Giáo viên chủ nhiệm", !classes.isEmpty() && missingHomeroom == 0, missingHomeroom == 0 ? "Tất cả lớp đã có GVCN." : missingHomeroom + " lớp chưa có GVCN."));

        long emptyClasses = classes.stream().filter(cls -> enrollmentRepository.findByClsIdAndAcademicYearIdAndStatus(cls.getId(), yearId, EnrollmentStatus.ACTIVE).isEmpty()).count();
        checks.add(check("STUDENTS", "Học sinh trong lớp", !classes.isEmpty() && emptyClasses == 0, emptyClasses == 0 ? "Tất cả lớp đã có học sinh." : emptyClasses + " lớp chưa có học sinh."));

        List<Semester> semesters = semesterRepository.findByAcademicYearId(yearId);
        long requiredSubjects = yearSubjectRepository.findByAcademicYearId(yearId).size();
        long missingAssignments = classes.stream().flatMap(cls -> semesters.stream().map(semester ->
            teachingRepository.findByClsIdAndSemesterIdAndStatus(cls.getId(), semester.getId(), AssignmentStatus.ACTIVE)
                .stream().map(item -> item.getSubject().getId()).distinct().count() < requiredSubjects)).filter(Boolean::booleanValue).count();
        checks.add(check("ASSIGNMENTS", "Phân công giảng dạy", !classes.isEmpty() && semesters.size() == 2 && missingAssignments == 0,
            missingAssignments == 0 ? "Mỗi lớp đã có phân công ở cả hai học kỳ." : missingAssignments + " cặp lớp/học kỳ chưa có phân công."));

        long duplicates = teachingRepository.findByAcademicYearId(yearId).stream()
            .collect(java.util.stream.Collectors.groupingBy(ta -> ta.getCls().getId() + "-" + ta.getSubject().getId() + "-" + ta.getSemester().getId()))
            .values().stream().mapToLong(items -> Math.max(0, items.size() - 1)).sum();
        checks.add(check("DUPLICATES", "Không trùng phân công", duplicates == 0, duplicates == 0 ? "Không có lớp/môn/học kỳ bị phân công trùng." : "Có " + duplicates + " phân công trùng."));
        return new AcademicYearReadinessDto(yearId, checks.stream().allMatch(ReadinessCheckDto::passed), checks);
    }

    @Override public void requireReady(Long yearId) {
        AcademicYearReadinessDto result = check(yearId);
        if (!result.ready()) throw new ConflictException("Năm học chưa đủ điều kiện kích hoạt: " + result.checks().stream().filter(item -> !item.passed()).map(ReadinessCheckDto::label).toList());
    }
    private ReadinessCheckDto check(String code, String label, boolean passed, String detail) { return new ReadinessCheckDto(code, label, passed, detail); }
}
