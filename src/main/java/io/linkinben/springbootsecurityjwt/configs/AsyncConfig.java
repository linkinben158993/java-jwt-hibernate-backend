package io.linkinben.springbootsecurityjwt.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.linkinben.springbootsecurityjwt.tracing.MdcTaskDecorator;

/**
 * Enables {@code @Async} (used by the non-critical auth audit listeners — R2/P3) and supplies the
 * executor. The {@link MdcTaskDecorator} carries the request's trace id onto the async thread so audit
 * log lines stay correlated (O7).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }
}
