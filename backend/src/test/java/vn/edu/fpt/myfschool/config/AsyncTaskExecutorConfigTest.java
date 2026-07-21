package vn.edu.fpt.myfschool.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncTaskExecutorConfigTest {

    @Test
    void registersBoundedExecutorUnderApplicationAndSpringDefaultNames() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AsyncTaskExecutorConfig.class)) {
            TaskExecutor applicationExecutor = context.getBean(
                    AsyncTaskExecutorConfig.APPLICATION_TASK_EXECUTOR, TaskExecutor.class);
            TaskExecutor defaultExecutor = context.getBean("taskExecutor", TaskExecutor.class);

            assertThat(defaultExecutor).isSameAs(applicationExecutor);
            assertThat(applicationExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);

            ThreadPoolTaskExecutor threadPool = (ThreadPoolTaskExecutor) applicationExecutor;
            assertThat(threadPool.getCorePoolSize()).isEqualTo(2);
            assertThat(threadPool.getMaxPoolSize()).isEqualTo(4);
            assertThat(threadPool.getQueueCapacity()).isEqualTo(500);
            assertThat(threadPool.getThreadNamePrefix()).isEqualTo("application-async-");
        }
    }
}
