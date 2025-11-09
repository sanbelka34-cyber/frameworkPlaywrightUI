package com.example.framework.config;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Immutable view of the framework configuration.
 */
public record FrameworkConfig(
        String baseUrl,
        String browser,
        boolean headless,
        int slowMo,
        int timeoutMs,
        boolean videoEnabled,
        Path videoDir,
        boolean traceEnabled,
        Path traceDir,
        Path screenshotsDir,
        Path downloadsDir,
        int parallelism
) {

    public Duration timeout() {
        return Duration.ofMillis(timeoutMs);
    }
}

