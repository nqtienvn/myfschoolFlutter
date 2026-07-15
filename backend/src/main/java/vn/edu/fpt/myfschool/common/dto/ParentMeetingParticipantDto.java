package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.MeetingAttendance;
import vn.edu.fpt.myfschool.common.enums.MeetingResponse;

import java.time.LocalDateTime;

public record ParentMeetingParticipantDto(
        Long guardianId,
        String guardianName,
        MeetingResponse response,
        MeetingAttendance attendance,
        LocalDateTime respondedAt
) {}
