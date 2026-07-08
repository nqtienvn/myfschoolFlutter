package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AcademicYearDto;
import vn.edu.fpt.myfschool.common.dto.CreateAcademicYearRequest;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.service.AcademicYearService;

import java.util.List;

@Service("academicYearService")
@RequiredArgsConstructor
@Transactional
public class AcademicYearServiceImpl implements AcademicYearService {

    private final AcademicYearRepository academicYearRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AcademicYearDto> listAcademicYears() {
        return academicYearRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public AcademicYearDto createAcademicYear(CreateAcademicYearRequest request) {
        if (academicYearRepository.existsByName(request.name())) {
            throw new ConflictException("Năm học đã tồn tại");
        }
        AcademicYear year = new AcademicYear();
        year.setName(request.name());
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        year.setStatus(request.status() != null ? request.status() : AcademicYearStatus.DRAFT);
        if (year.getStatus() == AcademicYearStatus.ACTIVE) deactivateActiveYears();
        return toDto(academicYearRepository.save(year));
    }

    @Override
    public AcademicYearDto updateAcademicYear(Long id, CreateAcademicYearRequest request) {
        AcademicYear year = findEntity(id);
        academicYearRepository.findByName(request.name())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> { throw new ConflictException("Năm học đã tồn tại"); });
        year.setName(request.name());
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        year.setStatus(request.status() != null ? request.status() : year.getStatus());
        if (year.getStatus() == AcademicYearStatus.ACTIVE) deactivateOtherActiveYears(id);
        return toDto(academicYearRepository.save(year));
    }

    @Override
    public AcademicYearDto updateStatus(Long id, AcademicYearStatus status) {
        AcademicYear year = findEntity(id);
        if (status == AcademicYearStatus.ACTIVE) deactivateOtherActiveYears(id);
        year.setStatus(status);
        return toDto(academicYearRepository.save(year));
    }

    @Override
    public AcademicYear findEntity(Long id) {
        return academicYearRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }

    private void deactivateActiveYears() {
        academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE)
            .forEach(year -> year.setStatus(AcademicYearStatus.CLOSED));
    }

    private void deactivateOtherActiveYears(Long activeId) {
        academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE).stream()
            .filter(year -> !year.getId().equals(activeId))
            .forEach(year -> year.setStatus(AcademicYearStatus.CLOSED));
    }

    private AcademicYearDto toDto(AcademicYear year) {
        return new AcademicYearDto(year.getId(), year.getName(), year.getStartDate(), year.getEndDate(), year.getStatus());
    }
}
