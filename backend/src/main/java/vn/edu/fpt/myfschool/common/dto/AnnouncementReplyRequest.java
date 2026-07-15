package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementReplyRequest(
    @NotBlank @Size(max = 1000) String replyText
) {}
