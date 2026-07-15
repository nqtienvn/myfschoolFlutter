package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.ParentMeetingStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ParentMeetingDto(
        Long id,
        String title,
        Long academicYearId,
        Long semesterId,
        Long classId,
        String className,
        Long studentId,
        String studentName,
        LocalDateTime startsAt,
        String location,
        String agenda,
        ParentMeetingStatus status,
        List<ParentMeetingParticipantDto> participants
) {}
