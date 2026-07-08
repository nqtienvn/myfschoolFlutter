package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.controller.entity.Semester;
import vn.edu.fpt.myfschool.mapper.SemesterMapper;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service("semesterService")
@RequiredArgsConstructor
@Transactional
public class SemesterServiceImpl implements SemesterService {

    private final SemesterRepository semesterRepository;
    private final SemesterMapper semesterMapper;

    @Transactional(readOnly = true)
    @Override
    public List<SemesterDto> listSemesters(String academicYear) {
        List<Semester> semesters;
        if (academicYear != null) {
            semesters = semesterRepository.findByAcademicYear(academicYear);
        } else {
            semesters = semesterRepository.findAll();
        }
        return semesters.stream().map(semesterMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public SemesterDto getCurrentSemester() {
        Semester semester = semesterRepository.findByIsCurrentTrue()
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
        if (semesterRepository.findByNameAndAcademicYear(request.name(), request.academicYear()).isPresent()) {
            throw new ConflictException("Học kỳ đã tồn tại trong năm học này");
        }
        Semester semester = new Semester();
        semester.setName(request.name());
        semester.setAcademicYear(request.academicYear());
        semester.setStartDate(request.startDate());
        semester.setEndDate(request.endDate());
        semester.setIsCurrent(request.isCurrent() != null ? request.isCurrent() : false);
        return semesterMapper.toDto(semesterRepository.save(semester));
    }

    @Override
    public SemesterDto updateSemester(Long id, CreateSemesterRequest request) {
        Semester semester = semesterRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", id));
        semester.setName(request.name());
        semester.setAcademicYear(request.academicYear());
        semester.setStartDate(request.startDate());
        semester.setEndDate(request.endDate());
        if (request.isCurrent() != null) semester.setIsCurrent(request.isCurrent());
        return semesterMapper.toDto(semesterRepository.save(semester));
    }

    @Override
    public void setCurrentSemester(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        // Reset all current
        semesterRepository.findByAcademicYear(semester.getAcademicYear())
            .forEach(s -> { s.setIsCurrent(false); semesterRepository.save(s); });
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
}
