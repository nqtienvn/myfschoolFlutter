package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;

public interface DashboardService {
    DashboardStudentStatsDto getStudentDashboard(
        Long requestUserId, Long studentId, Long academicYearId, Long semesterId);

    DashboardTeacherStatsDto getTeacherDashboard(
        Long teacherUserId, Long academicYearId, Long semesterId);
}
