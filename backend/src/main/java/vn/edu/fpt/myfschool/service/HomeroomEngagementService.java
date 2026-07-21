package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.util.List;

public interface HomeroomEngagementService {
    List<ParentContactLogDto> getContactLogs(Long studentId, Long academicYearId, Long semesterId,
            Long classId, Long teacherUserId);
    ParentContactLogDto createContactLog(Long studentId, SaveParentContactLogRequest request, Long teacherUserId);
    ParentContactLogDto updateContactLog(Long id, SaveParentContactLogRequest request, Long teacherUserId);
    void deleteContactLog(Long id, Long teacherUserId);

    List<ParentMeetingDto> getMeetings(Long academicYearId, Long semesterId, Long classId,
            Long requesterId, UserRole requesterRole);
    ParentMeetingDto createMeeting(SaveParentMeetingRequest request, Long teacherUserId);
    ParentMeetingDto updateMeeting(Long id, SaveParentMeetingRequest request, Long teacherUserId);
    ParentMeetingDto respondMeeting(Long id, MeetingResponseRequest request, Long parentUserId);
    ParentMeetingDto markAttendance(Long id, MeetingAttendanceRequest request, Long teacherUserId);

    List<StudentEventDto> getStudentEvents(Long studentId, Long academicYearId, Long semesterId,
            Long classId, Long requesterId, UserRole requesterRole);
    StudentEventDto createStudentEvent(Long studentId, SaveStudentEventRequest request,
            Long requesterId, UserRole requesterRole);
    StudentEventDto updateStudentEvent(Long id, SaveStudentEventRequest request,
            Long requesterId, UserRole requesterRole);
    void deleteStudentEvent(Long id, Long academicYearId, Long requesterId, UserRole requesterRole);
    List<StudentEventDto> submitStudentViolations(Long studentId, ViolationScopeRequest request,
            Long teacherUserId);
    List<StudentEventDto> submitClassViolations(ViolationScopeRequest request, Long teacherUserId);
}
