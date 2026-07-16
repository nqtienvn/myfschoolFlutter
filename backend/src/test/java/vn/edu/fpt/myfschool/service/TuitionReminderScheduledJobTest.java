package vn.edu.fpt.myfschool.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.PaymentConfiguration;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.TuitionBill;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.PaymentConfigurationRepository;
import vn.edu.fpt.myfschool.repository.StudentGuardianRepository;
import vn.edu.fpt.myfschool.repository.TuitionBillRepository;
import vn.edu.fpt.myfschool.scheduler.TuitionReminderScheduledJob;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TuitionReminderScheduledJobTest {
    @Mock PaymentConfigurationRepository configurations;
    @Mock TuitionBillRepository bills;
    @Mock StudentGuardianRepository guardians;
    @Mock NotificationService notifications;

    private TuitionReminderScheduledJob job;

    @BeforeEach
    void setUp() {
        job = new TuitionReminderScheduledJob(configurations, bills, guardians, notifications);
    }

    @Test
    void remindsStudentAndGuardianOncePerConfiguredIntervalWithinTheAcademicYear() {
        AcademicYear year = new AcademicYear();
        year.setId(101L);
        PaymentConfiguration configuration = new PaymentConfiguration();
        configuration.setAcademicYear(year);
        configuration.setReminderEnabled(true);
        configuration.setReminderIntervalDays(7);

        User studentUser = user(201L, "Học sinh");
        Student student = new Student();
        student.setId(301L);
        student.setUser(studentUser);
        User parentUser = user(202L, "Phụ huynh");
        Parent parent = new Parent();
        parent.setUser(parentUser);

        TuitionBill bill = new TuitionBill();
        bill.setId(401L);
        bill.setStudent(student);
        bill.setName("Học phí học kỳ I");
        bill.setAmount(new BigDecimal("12500000"));
        bill.setDueDate(LocalDate.of(2026, 7, 10));
        bill.setStatus(BillStatus.UNPAID);

        when(configurations.findAll()).thenReturn(List.of(configuration));
        when(bills.findByAcademicYearIdAndStatus(101L, BillStatus.UNPAID)).thenReturn(List.of(bill));
        when(bills.findByIdForUpdate(401L)).thenReturn(Optional.of(bill));
        when(guardians.findGuardiansByStudentId(301L)).thenReturn(List.of(parent));

        LocalDateTime firstRun = LocalDateTime.of(2026, 7, 17, 8, 0);
        assertThat(job.runReminders(firstRun)).isEqualTo(1);
        assertThat(bill.getLastReminderAt()).isEqualTo(firstRun);
        verify(notifications).createNotification(eq(201L), anyString(), anyString(), eq("Học phí"), eq(401L), eq("TUITION_BILL"));
        verify(notifications).createNotification(eq(202L), anyString(), anyString(), eq("Học phí"), eq(401L), eq("TUITION_BILL"));
        verify(bills).findByAcademicYearIdAndStatus(101L, BillStatus.UNPAID);

        clearInvocations(notifications);
        assertThat(job.runReminders(firstRun.plusDays(6))).isZero();
        verifyNoInteractions(notifications);
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }
}
