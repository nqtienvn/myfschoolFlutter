package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.controller.entity.AcademicYear;
import vn.edu.fpt.myfschool.controller.entity.Semester;
import vn.edu.fpt.myfschool.mapper.SemesterMapper;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.service.AcademicYearService;
import vn.edu.fpt.myfschool.service.SemesterService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service("semesterService")
@RequiredArgsConstructor
@Transactional
public class SemesterServiceImpl implements SemesterService {

    private final SemesterRepository semesterRepository;
    private final SemesterMapper semesterMapper;
    private final AcademicYearService academicYearService;

    @Transactional(readOnly = true)
    @Override
    public List<SemesterDto> listSemesters(Long academicYearId) {
        List<Semester> semesters = academicYearId != null
            ? semesterRepository.findByAcademicYearIdOrderByOrderAsc(academicYearId)
            : semesterRepository.findAll().stream()
                .sorted(Comparator.comparing((Semester s) -> s.getAcademicYear().getName()).thenComparing(Semester::getOrder))
                .toList();
        return semesters.stream().map(semesterMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public SemesterDto getCurrentSemester() {
        // ponytail: return active-year's current semester; global lookup breaks when multiple years have isCurrent=true
        List<Semester> allCurrent = semesterRepository.findAll().stream()
            .filter(Semester::getIsCurrent)
            .toList();
        Semester semester = allCurrent.stream()
            .filter(s -> s.getAcademicYear().getStatus() == AcademicYearStatus.ACTIVE)
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "isCurrent", true));
        return semesterMapper.toDto(semester);
    }

    @Transactional(readOnly = true)
    @Override
    public SemesterDto getSemester(Long id) {
        Semester semester = semesterRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", id));
        return semesterMapper.toDto(semester);
    }

    @Override
    public SemesterDto createSemester(CreateSemesterRequest request) {
        AcademicYear year = academicYearService.findEntity(request.academicYearId());
        if (semesterRepository.findByNameAndAcademicYearId(request.name(), year.getId()).isPresent()) {
            throw new ConflictException("Học kỳ đã tồn tại trong năm học này");
        }
        Semester semester = new Semester();
        semester.setName(request.name());
        semester.setAcademicYear(year);
        semester.setOrder(request.order());
        semester.setStartDate(request.startDate());
        semester.setEndDate(request.endDate());
        semester.setIsCurrent(request.isCurrent() != null ? request.isCurrent() : false);
        if (semester.getIsCurrent()) clearCurrentSemesters(year.getId());
        return semesterMapper.toDto(semesterRepository.save(semester));
    }

    @Override
    public SemesterDto updateSemester(Long id, CreateSemesterRequest request) {
        Semester semester = semesterRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", id));
        AcademicYear year = academicYearService.findEntity(request.academicYearId());
        semesterRepository.findByNameAndAcademicYearId(request.name(), year.getId())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> { throw new ConflictException("Học kỳ đã tồn tại trong năm học này"); });
        semester.setName(request.name());
        semester.setAcademicYear(year);
        semester.setOrder(request.order());
        semester.setStartDate(request.startDate());
        semester.setEndDate(request.endDate());
        if (request.isCurrent() != null && request.isCurrent()) {
            clearCurrentSemesters(year.getId());
            semester.setIsCurrent(true);
        } else if (request.isCurrent() != null) {
            semester.setIsCurrent(false);
        }
        return semesterMapper.toDto(semesterRepository.save(semester));
    }

    @Override
    public void setCurrentSemester(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        clearCurrentSemesters(semester.getAcademicYear().getId());
        semester.setIsCurrent(true);
        semesterRepository.save(semester);
    }

    @Override
    public void deleteSemester(Long id) {
        Semester semester = semesterRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", id));
        if (semester.getIsCurrent()) {
            throw new ConflictException("Không thể xóa học kỳ hiện tại đang hoạt động");
        }
        semesterRepository.delete(semester);
    }

    private void clearCurrentSemesters(Long academicYearId) {
        semesterRepository.findByAcademicYearId(academicYearId)
            .forEach(s -> { s.setIsCurrent(false); semesterRepository.save(s); });
    }
}
