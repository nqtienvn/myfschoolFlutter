package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.mapper.ClassMapper;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.AcademicYearService;
import vn.edu.fpt.myfschool.service.ClassService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service("classService")
@RequiredArgsConstructor
@Transactional
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AcademicYearService academicYearService;
    private final ClassMapper classMapper;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    @Transactional(readOnly = true)
    @Override
    public Page<ClassDto> listClasses(Long academicYearId, String keyword, int page, int size,
                                      Long requestUserId, UserRole requestRole) {
        Long yearId = resolveAcademicYearId(academicYearId);
        List<SchoolClass> classes;
        if (keyword != null && !keyword.isBlank()) {
            classes = classRepository.searchByYearAndKeyword(yearId, keyword.trim());
        } else {
            classes = classRepository.findByAcademicYearId(yearId);
        }
        Set<Long> visibleClassIds = visibleClassIds(yearId, requestUserId, requestRole);
        if (visibleClassIds != null) {
            classes = classes.stream()
                .filter(cls -> visibleClassIds.contains(cls.getId()))
                .toList();
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, size);
        int start = Math.min(safePage * safeSize, classes.size());
        int end = Math.min(start + safeSize, classes.size());
        List<ClassDto> dtos = classes.subList(start, end).stream()
            .map(classMapper::toDto).collect(Collectors.toList());
        return new PageImpl<>(dtos, PageRequest.of(safePage, safeSize), classes.size());
    }

    @Transactional(readOnly = true)
    @Override
    public ClassDetailDto getClassDetail(Long classId, Long requestUserId, UserRole requestRole) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        authorizeClassRosterAccess(cls, requestUserId, requestRole);
        
        Long yearId = cls.getAcademicYear().getId();

        // Students via Enrollment
        List<StudentSummaryDto> students = enrollmentRepository
            .findActiveStudentsByClassAndYear(classId, yearId)
            .stream().map(s -> new StudentSummaryDto(
                s.getId(), s.getUser().getName(), s.getStudentCode(),
                cls.getName()))
            .collect(Collectors.toList());

        // Teaching assignments
        List<TeachingAssignmentDto> assignments = teachingAssignmentRepository
            .findByClsIdAndStatus(classId, AssignmentStatus.ACTIVE)
            .stream().map(ta -> new TeachingAssignmentDto(
                ta.getId(),
                ta.getCls().getId(), ta.getCls().getName(), ta.getCls().getGradeLevel(),
                ta.getSubject().getId(), ta.getSubject().getName(), ta.getSubject().getCode(),
                ta.getTeacher().getId(), ta.getTeacher().getUser().getName(), ta.getTeacher().getEmployeeCode(),
                ta.getEffectiveFrom(), ta.getEffectiveTo(),
                ta.getStatus()))
            .collect(Collectors.toList());

        // Homeroom teacher
        HomeroomAssignmentDto homeroomDto = null;
        HomeroomAssignment ha = homeroomAssignmentRepository
            .findConfiguredByClassAndYear(classId, yearId).orElse(null);
        if (ha != null) {
            homeroomDto = new HomeroomAssignmentDto(
                ha.getId(),
                ha.getCls().getId(), ha.getCls().getName(),
                ha.getTeacher().getId(), ha.getTeacher().getUser().getName(),
                ha.getAcademicYear().getId(), ha.getAcademicYear().getName(),
                ha.getEffectiveFrom(), ha.getEffectiveTo());
        }

        return new ClassDetailDto(cls.getId(), cls.getName(), cls.getGradeLevel(),
            cls.getAcademicYear().getId(), cls.getAcademicYear().getName(),
            cls.getSchoolName(), students, assignments, homeroomDto);
    }

    @Override
    public List<ClassDto> generateClasses(GenerateClassesRequest request) {
        AcademicYear year = academicYearService.findEntity(request.academicYearId());
        requireDraft(year);
        String prefix = request.namingPrefix().trim().toUpperCase();
        List<String> names = java.util.stream.IntStream.rangeClosed(1, request.count())
            .mapToObj(index -> request.gradeLevel() + prefix + index).toList();
        names.stream().filter(name -> classRepository.existsByNameAndAcademicYearId(name, year.getId())).findFirst()
            .ifPresent(name -> { throw new ConflictException("Lớp " + name + " đã tồn tại trong năm học"); });
        return names.stream().map(name -> {
            SchoolClass cls = new SchoolClass(); cls.setName(name); cls.setGradeLevel(request.gradeLevel());
            cls.setAcademicYear(year); cls.setSchoolName("FPT Schools");
            return classMapper.toDto(classRepository.save(cls));
        }).toList();
    }

    @Override
    public ClassDto updateClass(Long classId, UpdateClassRequest request) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        AcademicYear year = academicYearService.findEntity(request.academicYearId());
        requireDraft(year);
        requireDraft(cls.getAcademicYear());
        classRepository.findByNameAndAcademicYearId(request.name(), year.getId())
            .filter(existing -> !existing.getId().equals(classId))
            .ifPresent(existing -> { throw new ConflictException("Lớp đã tồn tại trong năm học này"); });
        cls.setName(request.name());
        cls.setGradeLevel(request.gradeLevel());
        cls.setAcademicYear(year);
        if (request.schoolName() != null) cls.setSchoolName(request.schoolName());
        return classMapper.toDto(classRepository.save(cls));
    }

    @Override
    public void deleteClass(Long classId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        requireDraft(cls.getAcademicYear());
        if (!enrollmentRepository.findActiveStudentsByClassAndYear(classId, cls.getAcademicYear().getId()).isEmpty()) {
            throw new BadRequestException("Không thể xóa lớp có học sinh");
        }
        scheduleRepository.deleteByClassId(classId);
        teachingAssignmentRepository.deleteByClsId(classId);
        homeroomAssignmentRepository.deleteByClsId(classId);
        classRepository.delete(cls);
    }

    @Transactional(readOnly = true)
    @Override
    public List<StudentSummaryDto> getStudentsInClass(
            Long classId, Long requestUserId, UserRole requestRole) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        authorizeClassRosterAccess(cls, requestUserId, requestRole);
        return enrollmentRepository.findActiveStudentsByClassAndYear(classId, cls.getAcademicYear().getId())
            .stream().map(s -> new StudentSummaryDto(
                s.getId(), s.getUser().getName(), s.getStudentCode(),
                cls.getName()))
            .collect(Collectors.toList());
    }

    private Set<Long> visibleClassIds(Long academicYearId, Long requestUserId, UserRole requestRole) {
        if (requestRole == UserRole.ADMIN) return null;

        Set<Long> classIds = new LinkedHashSet<>();
        if (requestRole == UserRole.TEACHER) {
            Teacher teacher = teacherRepository.findByUserId(requestUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ giáo viên"));
            classIds.addAll(teachingAssignmentRepository
                .findClassIdsByTeacherAndYear(teacher.getId(), academicYearId));
            homeroomAssignmentRepository
                .findByTeacherIdAndAcademicYearId(teacher.getId(), academicYearId)
                .forEach(assignment -> classIds.add(assignment.getCls().getId()));
            return classIds;
        }

        if (requestRole == UserRole.STUDENT) {
            Student student = studentRepository.findByUserId(requestUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ học sinh"));
            enrollmentRepository.findByStudentId(student.getId()).stream()
                .filter(enrollment -> enrollment.getAcademicYear().getId().equals(academicYearId))
                .forEach(enrollment -> classIds.add(enrollment.getCls().getId()));
            return classIds;
        }

        if (requestRole == UserRole.PARENT) {
            Parent parent = parentRepository.findByUserId(requestUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ phụ huynh"));
            studentGuardianRepository.findByGuardianId(parent.getId()).stream()
                .map(link -> link.getStudent().getId())
                .flatMap(studentId -> enrollmentRepository.findByStudentId(studentId).stream())
                .filter(enrollment -> enrollment.getAcademicYear().getId().equals(academicYearId))
                .forEach(enrollment -> classIds.add(enrollment.getCls().getId()));
            return classIds;
        }

        throw new ForbiddenException("Tài khoản không có quyền xem danh sách lớp");
    }

    private void authorizeClassRosterAccess(
            SchoolClass cls, Long requestUserId, UserRole requestRole) {
        if (requestRole == UserRole.ADMIN) return;
        if (requestRole != UserRole.TEACHER) {
            throw new ForbiddenException("Chỉ Admin hoặc giáo viên được xem danh sách học sinh lớp");
        }

        Teacher teacher = teacherRepository.findByUserId(requestUserId)
            .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ giáo viên"));
        boolean subjectTeacher = teachingAssignmentRepository
            .findClassIdsByTeacherAndYear(teacher.getId(), cls.getAcademicYear().getId())
            .contains(cls.getId());
        boolean homeroomTeacher = homeroomAssignmentRepository
            .existsByTeacherIdAndClsIdAndAcademicYearId(
                teacher.getId(), cls.getId(), cls.getAcademicYear().getId());
        if (!subjectTeacher && !homeroomTeacher) {
            throw new ForbiddenException("Giáo viên chưa được phân công lớp này");
        }
    }

    private Long resolveAcademicYearId(Long academicYearId) {
        if (academicYearId != null) return academicYearId;
        return academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE).stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "status", AcademicYearStatus.ACTIVE))
            .getId();
    }

    private void requireDraft(AcademicYear year) {
        if (year.getStatus() != AcademicYearStatus.DRAFT) {
            throw new ConflictException("Chỉ được thay đổi lớp khi năm học ở trạng thái DRAFT");
        }
    }
}
