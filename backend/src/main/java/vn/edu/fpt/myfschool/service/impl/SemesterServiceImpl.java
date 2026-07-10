package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Semester;
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
        requireDraft(year);
        validateDates(year, request);
        if (request.order() < 1 || request.order() > 2) {
            throw new ConflictException("Năm học chỉ có Học kỳ 1 và Học kỳ 2");
        }
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
        requireDraft(year);
        if (!semester.getAcademicYear().getId().equals(year.getId())) {
            throw new ConflictException("Không được chuyển học kỳ sang năm học khác");
        }
        validateDates(year, request);
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
        if (semester.getAcademicYear().getStatus() != AcademicYearStatus.ACTIVE) {
            throw new ConflictException("Học kỳ hiện tại chỉ được thay đổi trong năm học ACTIVE");
        }
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
        throw new ConflictException("Hai học kỳ mặc định của năm học không được phép xóa");
    }

    private void requireDraft(AcademicYear year) {
        if (year.getStatus() != AcademicYearStatus.DRAFT) {
            throw new ConflictException("Chỉ được sửa học kỳ khi năm học ở trạng thái DRAFT");
        }
    }

    private void validateDates(AcademicYear year, CreateSemesterRequest request) {
        if (request.startDate().isBefore(year.getStartDate()) || request.endDate().isAfter(year.getEndDate())
                || !request.startDate().isBefore(request.endDate())) {
            throw new ConflictException("Thời gian học kỳ phải nằm trong năm học và ngày bắt đầu phải trước ngày kết thúc");
        }
    }

    private void clearCurrentSemesters(Long academicYearId) {
        semesterRepository.findByAcademicYearId(academicYearId)
            .forEach(s -> { s.setIsCurrent(false); semesterRepository.save(s); });
    }
}
