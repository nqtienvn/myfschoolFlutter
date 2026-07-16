package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.PaymentConfigurationDto;
import vn.edu.fpt.myfschool.common.dto.PaymentConfigurationRequest;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.PaymentConfiguration;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.PaymentConfigurationRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.service.PaymentConfigurationService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentConfigurationServiceImpl implements PaymentConfigurationService {

    private static final String METHOD = "BANK_TRANSFER";
    private static final String DISPLAY_MODE = "MANUAL";

    private final PaymentConfigurationRepository paymentConfigurationRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;

    @Override
    public PaymentConfigurationDto getByAcademicYear(Long academicYearId) {
        requireAcademicYear(academicYearId);
        return paymentConfigurationRepository.findByAcademicYearId(academicYearId)
            .map(this::toDto)
            .orElse(null);
    }

    @Override
    public PaymentConfigurationDto getBySemester(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        return paymentConfigurationRepository
            .findByAcademicYearId(semester.getAcademicYear().getId())
            .map(this::toDto)
            .orElse(null);
    }

    @Override
    @Transactional
    public PaymentConfigurationDto upsert(
            Long academicYearId, PaymentConfigurationRequest request) {
        AcademicYear academicYear = requireAcademicYear(academicYearId);
        if (academicYear.getStatus() == AcademicYearStatus.COMPLETED) {
            throw new ConflictException(
                "Không thể thay đổi cấu hình thanh toán của năm học đã hoàn tất");
        }
        validateTemplate(request.transferContentTemplate());

        PaymentConfiguration configuration = paymentConfigurationRepository
            .findByAcademicYearId(academicYearId)
            .orElseGet(PaymentConfiguration::new);
        configuration.setAcademicYear(academicYear);
        configuration.setBankCode(normalizeOptional(request.bankCode()));
        configuration.setBankName(request.bankName().trim());
        configuration.setAccountNumber(request.accountNumber().trim());
        configuration.setAccountHolder(request.accountHolder().trim());
        configuration.setBranch(normalizeOptional(request.branch()));
        configuration.setTransferContentTemplate(request.transferContentTemplate().trim());
        configuration.setEnabled(request.enabled());
        if (request.reminderEnabled() != null) {
            configuration.setReminderEnabled(request.reminderEnabled());
        }
        if (request.reminderIntervalDays() != null) {
            configuration.setReminderIntervalDays(request.reminderIntervalDays());
        }
        return toDto(paymentConfigurationRepository.save(configuration));
    }

    private AcademicYear requireAcademicYear(Long academicYearId) {
        return academicYearRepository.findById(academicYearId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "AcademicYear", "id", academicYearId));
    }

    private void validateTemplate(String template) {
        String trimmed = template.trim();
        if (!trimmed.contains("{studentCode}")) {
            throw new BadRequestException(
                "Nội dung chuyển khoản phải chứa biến {studentCode}");
        }
        String withoutSupportedVariables = trimmed
            .replace("{studentCode}", "")
            .replace("{academicYear}", "")
            .replace("{semester}", "");
        if (withoutSupportedVariables.contains("{")
                || withoutSupportedVariables.contains("}")) {
            throw new BadRequestException(
                "Nội dung chuyển khoản chứa biến không được hỗ trợ");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private PaymentConfigurationDto toDto(PaymentConfiguration configuration) {
        return new PaymentConfigurationDto(
            configuration.getId(),
            configuration.getAcademicYear().getId(),
            configuration.getBankCode(),
            configuration.getBankName(),
            configuration.getAccountNumber(),
            configuration.getAccountHolder(),
            configuration.getBranch(),
            configuration.getTransferContentTemplate(),
            configuration.getEnabled(),
            configuration.getReminderEnabled(),
            configuration.getReminderIntervalDays(),
            METHOD,
            DISPLAY_MODE,
            false,
            configuration.getUpdatedAt());
    }
}
