package com.example.blog.notification.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("loadtest")
public class ConfigurableNotificationHandlerDelayInjector implements NotificationHandlerDelayInjector {

    private final long delayMs;

    public ConfigurableNotificationHandlerDelayInjector(
            @Value("${app.notification.outbox.delay-ms:0}") long delayMs
    ) {
        this.delayMs = Math.max(delayMs, 0L);
    }

    @Override
    public void delayBeforeHandle() {
        if (delayMs <= 0L) {
            return;
        }

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Notification outbox delay injector interrupted. delayMs={}", delayMs, e);
        }
    }
}
