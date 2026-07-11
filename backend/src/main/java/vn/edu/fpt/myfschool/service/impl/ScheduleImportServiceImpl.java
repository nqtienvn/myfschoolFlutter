package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.myfschool.common.dto.ImportResultDto;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.enums.TimetableStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.util.ExcelReader;
import vn.edu.fpt.myfschool.entity.Period;
import vn.edu.fpt.myfschool.entity.Schedule;
import vn.edu.fpt.myfschool.entity.TeachingAssignment;
import vn.edu.fpt.myfschool.entity.Timetable;
import vn.edu.fpt.myfschool.repository.AcademicYearPeriodRepository;
import vn.edu.fpt.myfschool.repository.PeriodRepository;
import vn.edu.fpt.myfschool.repository.ScheduleRepository;
import vn.edu.fpt.myfschool.repository.TeachingAssignmentRepository;
import vn.edu.fpt.myfschool.repository.TimetableRepository;
import vn.edu.fpt.myfschool.service.ScheduleImportService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service("scheduleImportService")
@RequiredArgsConstructor
@Transactional
public class ScheduleImportServiceImpl implements ScheduleImportService {

    private final ScheduleRepository scheduleRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final TimetableRepository timetableRepository;
    private final PeriodRepository periodRepository;
    private final AcademicYearPeriodRepository academicYearPeriodRepository;
    private final ExcelReader excelReader;

    @Override
    public ImportResultDto importFromExcel(MultipartFile file, Long timetableId) {
        Timetable timetable = timetableRepository.findById(timetableId)
            .orElseThrow(() -> new ResourceNotFoundException("Timetable", "id", timetableId));
        if (timetable.getStatus() != TimetableStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được import vào thời khóa biểu nháp");
        }

        List<Map<String, String>> rows;
        try (InputStream is = file.getInputStream()) {
            rows = excelReader.read(is);
        } catch (Exception e) {
            return new ImportResultDto(0, 0, 0, List.of("Không thể đọc tệp Excel: " + e.getMessage()));
        }

        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            Map<String, String> row = rows.get(index);
            int rowNumber = index + 2;
            try {
                Long assignmentId = requiredLong(row, "assignmentId", "Mã phân công");
                Integer dayOfWeek = requiredLong(row, "dayOfWeek", "Thứ").intValue();
                Long periodId = requiredLong(row, "periodId", "Mã tiết");

                TeachingAssignment assignment = teachingAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + assignmentId));
                if (!assignment.getCls().getId().equals(timetable.getCls().getId())) {
                    throw new IllegalArgumentException("Phân công không thuộc lớp của thời khóa biểu");
                }
                Period period = periodRepository.findById(periodId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tiết học ID: " + periodId));
                Long academicYearId = timetable.getCls().getAcademicYear().getId();
                if (!academicYearPeriodRepository.existsByAcademicYearIdAndPeriodId(academicYearId, periodId)) {
                    throw new IllegalArgumentException("Tiết học không thuộc cấu hình năm học của lớp");
                }
                if (scheduleRepository.findByTimetableIdAndDayOfWeekAndPeriodRefId(
                        timetable.getId(), dayOfWeek, periodId).isPresent()) {
                    throw new IllegalArgumentException("Lớp đã có môn tại tiết này");
                }

                Schedule schedule = new Schedule();
                schedule.setTimetable(timetable);
                schedule.setAssignment(assignment);
                schedule.setDayOfWeek(dayOfWeek);
                schedule.setPeriod(period.getOrder());
                schedule.setPeriodRef(period);
                schedule.setRoom(timetable.getCls().getName());
                schedule.setShift(Shift.valueOf(period.getShift().getCode().toUpperCase(Locale.ROOT)));
                scheduleRepository.save(schedule);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Dòng " + rowNumber + ": " + e.getMessage());
            }
        }
        return new ImportResultDto(rows.size(), success, failed, errors);
    }

    private Long requiredLong(Map<String, String> row, String key, String label) {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " (" + key + ") trống");
        }
        return Long.parseLong(value.trim());
    }
}
