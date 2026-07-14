package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import java.util.List;

public interface AttendanceSessionService {
    AttendanceSessionDto createSession(CreateAttendanceSessionRequest request, Long userId);
    List<AttendanceDetailDto> updateDetails(UpdateAttendanceDetailRequest request, Long userId);
    AttendanceSessionDto closeSession(Long sessionId, Long userId);
    List<AttendanceSessionDto> findByClassDateShift(
        Long classId, java.time.LocalDate date,
        vn.edu.fpt.myfschool.common.enums.Shift shift, Long userId,
        vn.edu.fpt.myfschool.common.enums.UserRole userRole);
}
