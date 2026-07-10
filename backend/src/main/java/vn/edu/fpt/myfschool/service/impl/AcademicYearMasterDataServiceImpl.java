package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.AcademicYearMasterDataService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademicYearMasterDataServiceImpl implements AcademicYearMasterDataService {
    private final AcademicYearRepository academicYearRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolShiftRepository shiftRepository;
    private final PeriodRepository periodRepository;
    private final AcademicYearSubjectRepository yearSubjectRepository;
    private final AcademicYearShiftRepository yearShiftRepository;
    private final AcademicYearPeriodRepository yearPeriodRepository;

    @Override @Transactional(readOnly = true)
    public AcademicYearMasterDataConfigDto get(Long yearId) {
        requireYear(yearId);
        return new AcademicYearMasterDataConfigDto(yearId,
            yearSubjectRepository.findByAcademicYearId(yearId).stream().map(item -> item.getSubject().getId()).toList(),
            yearShiftRepository.findByAcademicYearId(yearId).stream().map(item -> item.getShift().getId()).toList(),
            yearPeriodRepository.findByAcademicYearId(yearId).stream().map(item -> item.getPeriod().getId()).toList());
    }

    @Override
    public AcademicYearMasterDataConfigDto update(Long yearId, UpdateAcademicYearMasterDataRequest request) {
        AcademicYear year = requireYear(yearId);
        if (year.getStatus() != AcademicYearStatus.DRAFT) throw new ConflictException("Chỉ được sửa danh mục khi năm học ở trạng thái DRAFT");
        List<Subject> subjects = requireAll(subjectRepository.findAllById(request.subjectIds()), request.subjectIds(), "Subject");
        List<SchoolShift> shifts = requireAll(shiftRepository.findAllById(request.shiftIds()), request.shiftIds(), "SchoolShift");
        List<Period> periods = requireAll(periodRepository.findAllById(request.periodIds()), request.periodIds(), "Period");
        if (periods.stream().anyMatch(period -> !request.shiftIds().contains(period.getShift().getId()))) {
            throw new ConflictException("Tiết học phải thuộc ca học đã chọn");
        }
        yearSubjectRepository.deleteByAcademicYearId(yearId);
        yearShiftRepository.deleteByAcademicYearId(yearId);
        yearPeriodRepository.deleteByAcademicYearId(yearId);
        subjects.forEach(subject -> { AcademicYearSubject item = new AcademicYearSubject(); item.setAcademicYear(year); item.setSubject(subject); yearSubjectRepository.save(item); });
        shifts.forEach(shift -> { AcademicYearShift item = new AcademicYearShift(); item.setAcademicYear(year); item.setShift(shift); yearShiftRepository.save(item); });
        periods.forEach(period -> { AcademicYearPeriod item = new AcademicYearPeriod(); item.setAcademicYear(year); item.setPeriod(period); yearPeriodRepository.save(item); });
        return get(yearId);
    }

    private AcademicYear requireYear(Long id) { return academicYearRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id)); }
    private <T> List<T> requireAll(List<T> values, List<Long> ids, String type) {
        if (values.size() != ids.stream().distinct().count()) throw new ResourceNotFoundException(type, "ids", ids);
        return values;
    }
}
