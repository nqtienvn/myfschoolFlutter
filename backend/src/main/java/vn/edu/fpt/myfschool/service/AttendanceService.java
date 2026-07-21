package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AttendanceCorrectionStatus;
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

    AttendanceCorrectionRequestDto requestAttendanceCorrection(
        CreateAttendanceCorrectionRequest request, Long teacherUserId);

    List<AttendanceCorrectionRequestDto> getAdminCorrections(
        Long academicYearId,
        AttendanceCorrectionStatus status,
        LocalDate date,
        Long classId,
        Long teacherId);

    long countPendingCorrections(Long academicYearId);

    List<AttendanceCorrectionRequestDto> getTeacherCorrectionHistory(
        Long academicYearId, Long teacherUserId);

    AttendanceCorrectionRequestDto reviewAttendanceCorrection(
        Long requestId, Long academicYearId, boolean approve, Long reviewerUserId);

}
