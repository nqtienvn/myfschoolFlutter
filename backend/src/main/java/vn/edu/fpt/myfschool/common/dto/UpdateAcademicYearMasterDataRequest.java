package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateAcademicYearMasterDataRequest(@NotNull List<Long> subjectIds, @NotNull List<Long> shiftIds, @NotNull List<Long> periodIds) {}
