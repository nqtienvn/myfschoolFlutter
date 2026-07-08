package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.controller.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.HomeroomAssignmentService;

import java.util.List;
import java.util.Optional;

@Service("homeroomAssignmentService")
@RequiredArgsConstructor
@Transactional
public class HomeroomAssignmentServiceImpl implements HomeroomAssignmentService {

    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicYearRepository academicYearRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HomeroomAssignmentDto> listByClass(Long classId, Long academicYearId) {
        return homeroomAssignmentRepository
            .findByClsIdAndAcademicYearId(classId, academicYearId)
            .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HomeroomAssignmentDto getById(Long id) {
        return toDto(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public HomeroomAssignmentDto getByClassAndYear(Long classId, Long academicYearId) {
        Optional<HomeroomAssignment> ha = homeroomAssignmentRepository
            .findActiveByClassAndYear(classId, academicYearId);
        return ha.map(this::toDto).orElse(null);
    }

    @Override
    public HomeroomAssignmentDto create(CreateHomeroomAssignmentRequest request) {
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        Teacher teacher = teacherRepository.findById(request.teacherId())
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.teacherId()));
        AcademicYear year = academicYearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", request.academicYearId()));

        Optional<HomeroomAssignment> existing = homeroomAssignmentRepository
            .findActiveByClassAndYear(request.classId(), request.academicYearId());
        if (existing.isPresent()) {
            throw new ConflictException("Lop da co GVCN, hay cap nhat thay vi tao moi");
        }

        HomeroomAssignment ha = new HomeroomAssignment();
        ha.setCls(cls);
        ha.setTeacher(teacher);
        ha.setAcademicYear(year);
        ha.setEffectiveFrom(request.effectiveFrom());
        return toDto(homeroomAssignmentRepository.save(ha));
    }

    @Override
    public HomeroomAssignmentDto update(Long id, CreateHomeroomAssignmentRequest request) {
        HomeroomAssignment ha = findEntity(id);
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        Teacher teacher = teacherRepository.findById(request.teacherId())
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.teacherId()));

        ha.setCls(cls);
        ha.setTeacher(teacher);
        ha.setEffectiveFrom(request.effectiveFrom());
        return toDto(homeroomAssignmentRepository.save(ha));
    }

    @Override
    public void delete(Long id) {
        HomeroomAssignment ha = findEntity(id);
        ha.setEffectiveTo(java.time.LocalDate.now());
        homeroomAssignmentRepository.save(ha);
    }

    private HomeroomAssignment findEntity(Long id) {
        return homeroomAssignmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("HomeroomAssignment", "id", id));
    }

    private HomeroomAssignmentDto toDto(HomeroomAssignment ha) {
        return new HomeroomAssignmentDto(
            ha.getId(),
            ha.getCls().getId(), ha.getCls().getName(),
            ha.getTeacher().getId(), ha.getTeacher().getUser().getName(),
            ha.getAcademicYear().getId(), ha.getAcademicYear().getName(),
            ha.getEffectiveFrom(), ha.getEffectiveTo()
        );
    }
}
