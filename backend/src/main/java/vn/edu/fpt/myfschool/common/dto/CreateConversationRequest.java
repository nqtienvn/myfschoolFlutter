package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;

public record CreateConversationRequest(@NotNull Long otherUserId) {}
