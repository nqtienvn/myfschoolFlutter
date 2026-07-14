package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.LeaveStatus;

import java.util.List;

public interface LeaveRequestService {
    LeaveRequestDto createLeaveRequest(CreateLeaveRequestRequest request, Long parentUserId);

    List<LeaveRequestDto> getParentLeaveRequests(
        Long parentUserId, Long studentId, Long semesterId);

    List<LeaveRequestDto> getStudentLeaveRequests(Long studentUserId);

    List<LeaveRequestDto> getPendingLeaveRequests(
        Long teacherUserId, Long academicYearId, Long semesterId);

    List<LeaveRequestDto> getReviewedLeaveRequests(
        Long teacherUserId, Long academicYearId, Long semesterId);

    List<LeaveRequestDto> getClassLeaveRequests(Long classId, LeaveStatus status, Long teacherUserId);

    LeaveRequestDto approveLeaveRequest(Long leaveRequestId, String response, Long teacherUserId);

    LeaveRequestDto rejectLeaveRequest(Long leaveRequestId, String response, Long teacherUserId);

    void cancelLeaveRequest(Long leaveRequestId, Long parentUserId);

    long getPendingCount(Long teacherUserId, Long academicYearId, Long semesterId);
}
