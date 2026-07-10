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
import java.util.Set;
import java.util.stream.Collectors;

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
        syncSubjects(year, subjects);
        syncShifts(year, shifts);
        syncPeriods(year, periods);
        return get(yearId);
    }

    private void syncSubjects(AcademicYear year, List<Subject> requested) {
        List<AcademicYearSubject> current = yearSubjectRepository.findByAcademicYearId(year.getId());
        Set<Long> requestedIds = requested.stream().map(Subject::getId).collect(Collectors.toSet());
        Set<Long> currentIds = current.stream().map(item -> item.getSubject().getId()).collect(Collectors.toSet());
        yearSubjectRepository.deleteAll(current.stream()
            .filter(item -> !requestedIds.contains(item.getSubject().getId())).toList());
        yearSubjectRepository.saveAll(requested.stream()
            .filter(subject -> !currentIds.contains(subject.getId()))
            .map(subject -> { AcademicYearSubject item = new AcademicYearSubject(); item.setAcademicYear(year); item.setSubject(subject); return item; })
            .toList());
    }

    private void syncShifts(AcademicYear year, List<SchoolShift> requested) {
        List<AcademicYearShift> current = yearShiftRepository.findByAcademicYearId(year.getId());
        Set<Long> requestedIds = requested.stream().map(SchoolShift::getId).collect(Collectors.toSet());
        Set<Long> currentIds = current.stream().map(item -> item.getShift().getId()).collect(Collectors.toSet());
        yearShiftRepository.deleteAll(current.stream()
            .filter(item -> !requestedIds.contains(item.getShift().getId())).toList());
        yearShiftRepository.saveAll(requested.stream()
            .filter(shift -> !currentIds.contains(shift.getId()))
            .map(shift -> { AcademicYearShift item = new AcademicYearShift(); item.setAcademicYear(year); item.setShift(shift); return item; })
            .toList());
    }

    private void syncPeriods(AcademicYear year, List<Period> requested) {
        List<AcademicYearPeriod> current = yearPeriodRepository.findByAcademicYearId(year.getId());
        Set<Long> requestedIds = requested.stream().map(Period::getId).collect(Collectors.toSet());
        Set<Long> currentIds = current.stream().map(item -> item.getPeriod().getId()).collect(Collectors.toSet());
        yearPeriodRepository.deleteAll(current.stream()
            .filter(item -> !requestedIds.contains(item.getPeriod().getId())).toList());
        yearPeriodRepository.saveAll(requested.stream()
            .filter(period -> !currentIds.contains(period.getId()))
            .map(period -> { AcademicYearPeriod item = new AcademicYearPeriod(); item.setAcademicYear(year); item.setPeriod(period); return item; })
            .toList());
    }

    private AcademicYear requireYear(Long id) { return academicYearRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id)); }
    private <T> List<T> requireAll(List<T> values, List<Long> ids, String type) {
        if (values.size() != ids.stream().distinct().count()) throw new ResourceNotFoundException(type, "ids", ids);
        return values;
    }
}
