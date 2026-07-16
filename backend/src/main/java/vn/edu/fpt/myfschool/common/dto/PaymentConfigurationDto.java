package vn.edu.fpt.myfschool.common.dto;

import java.time.LocalDateTime;

public record PaymentConfigurationDto(
    Long id,
    Long academicYearId,
    String bankCode,
    String bankName,
    String accountNumber,
    String accountHolder,
    String branch,
    String transferContentTemplate,
    Boolean enabled,
    Boolean reminderEnabled,
    Integer reminderIntervalDays,
    String method,
    String displayMode,
    Boolean qrAvailable,
    LocalDateTime updatedAt
) {}
