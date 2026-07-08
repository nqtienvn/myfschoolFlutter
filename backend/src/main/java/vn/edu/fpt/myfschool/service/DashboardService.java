package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;

public interface DashboardService {
    DashboardStudentStatsDto getStudentDashboard(Long studentUserId);
}
