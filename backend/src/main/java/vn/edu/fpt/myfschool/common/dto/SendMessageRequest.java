package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.myfschool.common.enums.MessageType;

public record SendMessageRequest(
        @NotBlank
        @Size(max = 80)
        String clientMessageId,

        @NotBlank
        @Size(max = 4000)
        String content,

        MessageType messageType
) {}