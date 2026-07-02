package vn.edu.fpt.myfschool.common.dto;

import java.time.LocalDateTime;

public record SearchResultDto(
        Long id,
        String name,
        String phone,
        String avatar,
        String role
) {}