package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {
    DailyAttendanceDto getDailyAttendance(Long classId, LocalDate date, Shift shift, Long teacherUserId);

    List<AttendanceDto> submitAttendance(SubmitAttendanceRequest request, Long teacherUserId);

    AttendanceDto updateAttendance(Long attendanceId, AttendanceStatus newStatus, Long teacherUserId);

    AttendanceLogDto getStudentAttendanceLog(Long studentId, Long semesterId);

    void autoUpdateForApprovedLeave(Long leaveRequestId);
}
