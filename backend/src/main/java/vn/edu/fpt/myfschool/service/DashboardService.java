package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import java.util.List;
import java.util.stream.Collectors;

public interface DashboardService {
    DashboardStudentStatsDto getStudentDashboard(Long studentUserId);
}
