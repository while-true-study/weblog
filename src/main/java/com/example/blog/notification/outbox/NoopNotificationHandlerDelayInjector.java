package com.example.blog.notification.outbox;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!loadtest")
public class NoopNotificationHandlerDelayInjector implements NotificationHandlerDelayInjector {

    @Override
    public void delayBeforeHandle() {
    }
}
