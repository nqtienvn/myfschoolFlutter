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

        var requiredSubjects = yearSubjectRepository.findByAcademicYearId(yearId);
        List<String> missingAssignments = new ArrayList<>();
        for (SchoolClass cls : classes) {
            var assignedSubjectIds = teachingRepository.findByClsIdAndStatus(cls.getId(), AssignmentStatus.ACTIVE).stream()
                .map(item -> item.getSubject().getId()).collect(java.util.stream.Collectors.toSet());
            requiredSubjects.stream()
                .filter(item -> !assignedSubjectIds.contains(item.getSubject().getId()))
                .forEach(item -> missingAssignments.add(cls.getName() + " – " + item.getSubject().getName()));
        }
        String assignmentDetail = missingAssignments.isEmpty()
            ? "Mỗi lớp đã có đủ giáo viên cho các môn áp dụng trong năm học."
            : "Còn thiếu " + missingAssignments.size() + " phân công lớp/môn: "
                + String.join(", ", missingAssignments.stream().limit(5).toList())
                + (missingAssignments.size() > 5 ? "…" : ".");
        checks.add(check("ASSIGNMENTS", "Phân công giảng dạy",
            !classes.isEmpty() && !requiredSubjects.isEmpty() && missingAssignments.isEmpty(), assignmentDetail));

        long duplicates = teachingRepository.findByAcademicYearId(yearId).stream()
            .collect(java.util.stream.Collectors.groupingBy(ta -> ta.getCls().getId() + "-" + ta.getSubject().getId()))
            .values().stream().filter(items -> items.stream().map(item -> item.getTeacher().getId()).distinct().count() > 1).count();
        checks.add(check("DUPLICATES", "Không trùng phân công", duplicates == 0,
            duplicates == 0 ? "Không có lớp/môn bị phân cho nhiều giáo viên trong cùng năm học." : "Có " + duplicates + " lớp/môn đang có nhiều giáo viên."));
        return new AcademicYearReadinessDto(yearId, checks.stream().allMatch(ReadinessCheckDto::passed), checks);
    }

    @Override public void requireReady(Long yearId) {
        AcademicYearReadinessDto result = check(yearId);
        if (!result.ready()) throw new ConflictException("Năm học chưa đủ điều kiện kích hoạt: " + result.checks().stream().filter(item -> !item.passed()).map(ReadinessCheckDto::label).toList());
    }
    private ReadinessCheckDto check(String code, String label, boolean passed, String detail) { return new ReadinessCheckDto(code, label, passed, detail); }
}
