package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.myfschool.common.enums.MeetingResponse;

public record MeetingResponseRequest(@NotNull MeetingResponse response) {}
