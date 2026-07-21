package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.edu.fpt.myfschool.config.AsyncTaskExecutorConfig;
import vn.edu.fpt.myfschool.entity.AuditLog;
import vn.edu.fpt.myfschool.repository.AuditLogRepository;
import vn.edu.fpt.myfschool.service.AuditLogService;

@Service("auditLogService")
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async(AsyncTaskExecutorConfig.APPLICATION_TASK_EXECUTOR)
    @Override
    public void save(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.warn("Không thể lưu audit log cho {} {}", auditLog.getHttpMethod(), auditLog.getUri(), ex);
        }
    }
}
