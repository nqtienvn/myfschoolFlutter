package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record AcademicYearReadinessDto(Long academicYearId, boolean ready, List<ReadinessCheckDto> checks) {}
