package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.UserRole;

public record ParticipantDto(Long userId, String name, String avatar, UserRole role) {}
