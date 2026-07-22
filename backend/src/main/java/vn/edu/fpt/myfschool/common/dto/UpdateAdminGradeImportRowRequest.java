package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpdateAdminGradeImportRowRequest(
        @NotEmpty List<@Valid UpdateAdminGradeImportCellRequest> cells
) {}
