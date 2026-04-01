package com.example.blog.global.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class TraceHelper {

    private final ObservationRegistry observationRegistry;

    public <T> T trace(String name, Supplier<T> supplier) {
        return Observation.createNotStarted(name, observationRegistry)
                .observe(supplier::get);
    }

    public void trace(String name, Runnable runnable) {
        Observation.createNotStarted(name, observationRegistry)
                .observe(() -> {
                    runnable.run();
                    return null;
                });
    }
}