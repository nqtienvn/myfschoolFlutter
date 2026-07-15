package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.Shift;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {
    HomeroomAttendanceContextDto getHomeroomContext(LocalDate date, Long teacherUserId);

    DailyAttendanceDto getDailyAttendance(Long classId, LocalDate date, Shift shift, Long teacherUserId);

    List<AttendanceDto> submitAttendance(SubmitAttendanceRequest request, Long teacherUserId);

    /**
     * Synchronizes the compatibility attendance-session flow into the canonical
     * attendance table. Unlike the first submission endpoint, this method may
     * update an existing class/date/shift roster.
     */
    List<AttendanceDto> synchronizeSessionAttendance(
        SubmitAttendanceRequest request, Long teacherUserId);

    AttendanceLogDto getStudentAttendanceLog(Long studentId, Long semesterId, Long requestUserId);

    List<ClassAttendanceSummaryDto> getClassAttendanceSummary(
        Long classId, Long semesterId, Long academicYearId);

    List<AdminAttendanceDayDto> getAdminDailyAttendance(Long academicYearId, LocalDate date);

    AdminAttendanceDayDto adjustAdminDailyAttendance(AdminAttendanceAdjustmentRequest request);

    AttendanceCorrectionRequestDto requestAttendanceCorrection(
        CreateAttendanceCorrectionRequest request, Long teacherUserId);

    List<AttendanceCorrectionRequestDto> getPendingCorrections(Long academicYearId, LocalDate date);

    List<AttendanceCorrectionRequestDto> getTeacherCorrectionHistory(
        Long academicYearId, Long teacherUserId);

    List<AttendanceCorrectionRequestDto> getAdminCorrectionHistory(
        Long academicYearId, LocalDate date);

    AttendanceCorrectionRequestDto reviewAttendanceCorrection(
        Long requestId, boolean approve, Long reviewerUserId);

}
