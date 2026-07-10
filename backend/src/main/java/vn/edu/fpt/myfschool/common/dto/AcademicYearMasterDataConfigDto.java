package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record AcademicYearMasterDataConfigDto(Long academicYearId, List<Long> subjectIds, List<Long> shiftIds, List<Long> periodIds) {}
