package vn.edu.fpt.myfschool.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;
import vn.edu.fpt.myfschool.config.MailProperties;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MailDeliveryServiceTest {

    @Test
    void retriesSamePasswordResetMessageWithoutCreatingAnotherRequest() {
        MailGateway gateway = mock(MailGateway.class);
        doThrow(new IllegalStateException("temporary Gmail error"))
                .doThrow(new IllegalStateException("temporary Gmail error"))
                .doNothing()
                .when(gateway).sendPasswordResetEmail("parent@test.example", "https://example/reset#token=opaque");

        MailProperties properties = new MailProperties();
        properties.setMaxAttempts(3);
        properties.setRetryDelayMillis(0);
        MailDeliveryService service = new MailDeliveryService(gateway, new SyncTaskExecutor(), properties);

        service.sendPasswordResetAfterCommit("parent@test.example", "https://example/reset#token=opaque");

        verify(gateway, org.mockito.Mockito.times(3))
                .sendPasswordResetEmail("parent@test.example", "https://example/reset#token=opaque");
    }
}
