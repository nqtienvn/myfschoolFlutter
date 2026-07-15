package vn.edu.fpt.myfschool.common.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
public record AdminAnnouncementRequest(
    @NotBlank String title,
    @NotBlank String body,
    @NotNull Long academicYearId,
    String recipientScope,
    TargetRole targetRole,
    List<Long> classIds,
    String teacherAudience,
    Long subjectId,
    Boolean requiresReply
) {}
