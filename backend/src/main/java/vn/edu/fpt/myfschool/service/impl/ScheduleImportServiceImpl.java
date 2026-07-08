package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.myfschool.common.dto.ImportResultDto;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.util.ExcelReader;
import vn.edu.fpt.myfschool.entity.Schedule;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.TeachingAssignment;
import vn.edu.fpt.myfschool.repository.ScheduleRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.repository.TeachingAssignmentRepository;
import vn.edu.fpt.myfschool.service.ScheduleImportService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service("scheduleImportService")
@RequiredArgsConstructor
@Transactional
public class ScheduleImportServiceImpl implements ScheduleImportService {

    private final ScheduleRepository scheduleRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final SemesterRepository semesterRepository;
    private final ExcelReader excelReader;

    @Override
    public ImportResultDto importFromExcel(MultipartFile file, Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));

        List<Map<String, String>> rows;
        try (InputStream is = file.getInputStream()) {
            rows = excelReader.read(is);
        } catch (Exception e) {
            return new ImportResultDto(0, 0, 0, List.of("Không thể đọc tệp Excel: " + e.getMessage()));
        }

        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2;
            try {
                String assignmentIdStr = row.get("assignmentId");
                String dayOfWeekStr = row.get("dayOfWeek");
                String periodStr = row.get("period");
                String room = row.getOrDefault("room", "").trim();
                String shiftStr = row.getOrDefault("shift", "MORNING").trim();

                if (assignmentIdStr == null || assignmentIdStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Mã phân công (assignmentId) trống");
                }
                if (dayOfWeekStr == null || dayOfWeekStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Thứ (dayOfWeek) trống");
                }
                if (periodStr == null || periodStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Tiết (period) trống");
                }

                Long assignmentId = Long.parseLong(assignmentIdStr.trim());
                Integer dayOfWeek = Integer.parseInt(dayOfWeekStr.trim());
                Integer period = Integer.parseInt(periodStr.trim());
                Shift shift = "AFTERNOON".equalsIgnoreCase(shiftStr) ? Shift.AFTERNOON : Shift.MORNING;

                TeachingAssignment ta = teachingAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công giảng dạy với ID: " + assignmentId));

                if (!ta.getSemester().getId().equals(semester.getId())) {
                    throw new IllegalArgumentException("Phân công giảng dạy này thuộc học kỳ khác (" + ta.getSemester().getName() + ")");
                }

                if (scheduleRepository.findByClassIdAndSemesterIdAndDayOfWeekAndPeriod(
                        ta.getCls().getId(), semester.getId(), dayOfWeek, period).isPresent()) {
                    throw new IllegalArgumentException(String.format("Trùng lịch học: Lớp %s đã có tiết %d vào thứ %d",
                        ta.getCls().getName(), period, dayOfWeek));
                }

                if (ta.getTeacher() != null) {
                    if (scheduleRepository.findTeacherConflict(
                            ta.getTeacher().getId(), semester.getId(), dayOfWeek, period).isPresent()) {
                        throw new IllegalArgumentException(String.format("Trùng lịch dạy: Giáo viên %s đã có tiết dạy khác vào tiết %d thứ %d",
                            ta.getTeacher().getUser().getName(), period, dayOfWeek));
                    }
                }

                if (scheduleRepository.findByAssignmentIdAndDayOfWeekAndPeriod(assignmentId, dayOfWeek, period).isPresent()) {
                    throw new IllegalArgumentException(String.format("Tiết học này cho phân công %d đã tồn tại", assignmentId));
                }

                Schedule schedule = new Schedule();
                schedule.setAssignment(ta);
                schedule.setDayOfWeek(dayOfWeek);
                schedule.setPeriod(period);
                schedule.setRoom(room);
                schedule.setShift(shift);
                scheduleRepository.save(schedule);

                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Dòng " + rowNum + ": " + e.getMessage());
            }
        }

        return new ImportResultDto(rows.size(), success, failed, errors);
    }
}
