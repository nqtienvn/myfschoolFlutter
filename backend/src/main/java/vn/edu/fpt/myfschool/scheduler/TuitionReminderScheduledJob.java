package vn.edu.fpt.myfschool.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.entity.PaymentConfiguration;
import vn.edu.fpt.myfschool.entity.TuitionBill;
import vn.edu.fpt.myfschool.repository.PaymentConfigurationRepository;
import vn.edu.fpt.myfschool.repository.StudentGuardianRepository;
import vn.edu.fpt.myfschool.repository.TuitionBillRepository;
import vn.edu.fpt.myfschool.service.NotificationService;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional
public class TuitionReminderScheduledJob {
    private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentConfigurationRepository configurations;
    private final TuitionBillRepository bills;
    private final StudentGuardianRepository guardians;
    private final NotificationService notifications;

    @Scheduled(
            cron = "${app.tuition-reminder.cron:0 0 8 * * *}",
            zone = "${app.school-zone:Asia/Ho_Chi_Minh}")
    public void runScheduled() {
        runReminders(LocalDateTime.now(SCHOOL_ZONE));
    }

    public int runReminders(LocalDateTime now) {
        int reminded = 0;
        for (PaymentConfiguration configuration : configurations.findAll()) {
            if (!Boolean.TRUE.equals(configuration.getReminderEnabled())) continue;
            int intervalDays = Math.max(1, configuration.getReminderIntervalDays());
            Long academicYearId = configuration.getAcademicYear().getId();
            for (TuitionBill candidate : bills.findByAcademicYearIdAndStatus(
                    academicYearId, BillStatus.UNPAID)) {
                TuitionBill bill = bills.findByIdForUpdate(candidate.getId()).orElse(null);
                if (bill == null || bill.getStatus() != BillStatus.UNPAID
                        || bill.getDueDate().isAfter(now.toLocalDate())) continue;
                if (bill.getLastReminderAt() != null
                        && bill.getLastReminderAt().plusDays(intervalDays).isAfter(now)) continue;
                sendReminder(bill);
                bill.setLastReminderAt(now);
                bills.save(bill);
                reminded++;
            }
        }
        return reminded;
    }

    private void sendReminder(TuitionBill bill) {
        String title = "Nhắc thanh toán học phí";
        String body = bill.getName() + " đã đến hạn " + bill.getDueDate()
                + ". Số tiền cần thanh toán: " + bill.getAmount().toPlainString() + " VNĐ.";
        notifications.createNotification(bill.getStudent().getUser().getId(), title, body,
                "Học phí", bill.getId(), "TUITION_BILL");
        guardians.findGuardiansByStudentId(bill.getStudent().getId()).forEach(parent ->
                notifications.createNotification(parent.getUser().getId(), title, body,
                        "Học phí", bill.getId(), "TUITION_BILL"));
    }
}
