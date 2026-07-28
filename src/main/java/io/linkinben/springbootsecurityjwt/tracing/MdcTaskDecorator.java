package io.linkinben.springbootsecurityjwt.tracing;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * Copies the request thread's MDC (trace id, uId) onto an async worker thread so log lines emitted by
 * {@code @Async} listeners carry the same trace id (observability O7). Without this, an async thread
 * starts with an empty MDC and its logs would show a blank id.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        Map<String, String> context = MDC.getCopyOfContextMap(); // captured on the request thread
        return () -> {
            if (context != null) {
                MDC.setContextMap(context); // restored on the async thread
            }
            try {
                task.run();
            } finally {
                MDC.clear(); // never leak the copied context across pooled async threads
            }
        };
    }
}
