package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.SemesterResult;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.repository.*;

import java.util.List;
import java.util.stream.Collectors;

@Service("semesterResultService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemesterResultServiceImpl implements SemesterResultService {

    private final SemesterResultRepository semesterResultRepository;
    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final TeacherRepository teacherRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;

    @Override
    public SemesterResultDto getStudentSemesterResult(
            Long studentId, Long semesterId, Long requestUserId, UserRole requestRole) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        SemesterResult sr = semesterResultRepository.findByStudentIdAndSemesterId(studentId, semesterId)
            .orElse(null);
        SchoolClass resultClass;
        if (sr != null) {
            if (!sr.getCls().getAcademicYear().getId()
                    .equals(semester.getAcademicYear().getId())) {
                throw new BadRequestException("Lớp của kết quả không thuộc năm học đã chọn");
            }
            boolean enrolledInResultClass = enrollmentRepository.findByStudentId(studentId)
                .stream()
                .anyMatch(enrollment -> enrollment.getCls().getId().equals(sr.getCls().getId())
                    && enrollment.getAcademicYear().getId()
                        .equals(semester.getAcademicYear().getId()));
            if (!enrolledInResultClass) {
                throw new BadRequestException("Kết quả học kỳ không khớp hồ sơ nhập học");
            }
            resultClass = sr.getCls();
        } else {
            Enrollment enrollment = enrollmentRepository
                .findFirstByStudentIdAndAcademicYearIdOrderByIdDesc(
                    studentId, semester.getAcademicYear().getId())
                .orElseThrow(() -> new BadRequestException(
                    "Học sinh không thuộc năm học của học kỳ đã chọn"));
            resultClass = enrollment.getCls();
        }
        authorizeStudentResult(
            student, resultClass, semester, requestUserId, requestRole);

        if (sr == null) return null;

        return new SemesterResultDto(sr.getId(), student.getId(), student.getUser().getName(),
            semester.getId(), semester.getName(), sr.getCls().getId(), sr.getCls().getName(),
            sr.getGpa(), sr.getRank(), sr.getHonor(), sr.getConduct(), sr.getAcademicAbility());
    }

    @Override
    public ClassRankingDto getClassRanking(
            Long classId, Long semesterId, Long requestUserId, UserRole requestRole) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!semester.getAcademicYear().getId().equals(cls.getAcademicYear().getId())) {
            throw new BadRequestException("Học kỳ không thuộc năm học của lớp đã chọn");
        }
        authorizeClassSummary(cls, semester, requestUserId, requestRole);

        List<SemesterResult> results = semesterResultRepository.findByClassIdAndSemesterIdOrderByRankAsc(classId, semesterId);
        List<ClassRankEntryDto> rankings = results.stream().map(sr ->
            new ClassRankEntryDto(sr.getRank(), sr.getStudent().getId(),
                sr.getStudent().getUser().getName(), sr.getStudent().getStudentCode(),
                sr.getGpa(), sr.getAcademicAbility(), sr.getConduct())
        ).collect(Collectors.toList());

        return new ClassRankingDto(classId, cls.getName(), semesterId, semester.getName(), rankings);
    }

    private void authorizeStudentResult(
            Student student, SchoolClass cls, Semester semester,
            Long requestUserId, UserRole requestRole) {
        if (requestRole == UserRole.ADMIN) return;
        if (requestRole == UserRole.STUDENT) {
            if (!student.getUser().getId().equals(requestUserId)) {
                throw new ForbiddenException("Học sinh chỉ được xem kết quả của chính mình");
            }
            return;
        }
        if (requestRole == UserRole.PARENT) {
            Parent parent = parentRepository.findByUserId(requestUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ phụ huynh"));
            if (!studentGuardianRepository.existsByStudentIdAndGuardianId(
                    student.getId(), parent.getId())) {
                throw new ForbiddenException("Phụ huynh không quản lý học sinh này");
            }
            return;
        }
        authorizeClassSummary(cls, semester, requestUserId, requestRole);
    }

    private void authorizeClassSummary(
            SchoolClass cls, Semester semester, Long requestUserId, UserRole requestRole) {
        if (requestRole == UserRole.ADMIN) return;
        if (requestRole != UserRole.TEACHER) {
            throw new ForbiddenException("Chỉ GVCN được xem tổng kết của cả lớp");
        }
        Teacher teacher = teacherRepository.findByUserId(requestUserId)
            .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ giáo viên"));
        boolean assigned = homeroomAssignmentRepository
            .findByTeacherIdAndAcademicYearId(
                teacher.getId(), semester.getAcademicYear().getId())
            .stream()
            .filter(assignment -> assignment.getCls().getId().equals(cls.getId()))
            .anyMatch(assignment -> overlaps(assignment, semester));
        if (!assigned) {
            throw new ForbiddenException(
                "Giáo viên không phải GVCN của lớp trong học kỳ đã chọn");
        }
    }

    private boolean overlaps(HomeroomAssignment assignment, Semester semester) {
        return !assignment.getEffectiveFrom().isAfter(semester.getEndDate())
            && (assignment.getEffectiveTo() == null
                || !assignment.getEffectiveTo().isBefore(semester.getStartDate()));
    }
}
