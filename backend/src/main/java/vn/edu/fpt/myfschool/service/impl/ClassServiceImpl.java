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
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.mapper.ClassMapper;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.AcademicYearService;
import vn.edu.fpt.myfschool.service.ClassService;

import java.util.List;
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

    @Transactional(readOnly = true)
    @Override
    public Page<ClassDto> listClasses(Long academicYearId, String keyword, int page, int size) {
        Long yearId = resolveAcademicYearId(academicYearId);
        List<SchoolClass> classes;
        if (keyword != null && !keyword.isBlank()) {
            classes = classRepository.searchByYearAndKeyword(yearId, keyword.trim());
        } else {
            classes = classRepository.findByAcademicYearId(yearId);
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
    public ClassDetailDto getClassDetail(Long classId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        
        Long yearId = cls.getAcademicYear().getId();

        // Students via Enrollment
        List<StudentSummaryDto> students = enrollmentRepository
            .findActiveStudentsByClassAndYear(classId, yearId)
            .stream().map(s -> new StudentSummaryDto(
                s.getId(), s.getUser().getName(), s.getStudentCode(),
                cls.getName(), s.getUser().getAvatar()))
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
            .findActiveByClassAndYear(classId, yearId).orElse(null);
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
    public List<StudentSummaryDto> getStudentsInClass(Long classId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        return enrollmentRepository.findActiveStudentsByClassAndYear(classId, cls.getAcademicYear().getId())
            .stream().map(s -> new StudentSummaryDto(
                s.getId(), s.getUser().getName(), s.getStudentCode(),
                cls.getName(), s.getUser().getAvatar()))
            .collect(Collectors.toList());
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
