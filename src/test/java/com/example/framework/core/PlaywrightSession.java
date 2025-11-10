package com.example.framework.core;

import com.example.framework.config.FrameworkConfig;
import com.example.framework.core.support.FileSystemSupport;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.Video;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents an isolated Playwright session for a single test execution.
 */
public final class PlaywrightSession implements AutoCloseable {

    private final String testId;
    private final FrameworkConfig config;
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;
    private boolean tracingStarted;

    PlaywrightSession(String testId,
                      FrameworkConfig config,
                      Playwright playwright,
                      Browser browser,
                      BrowserContext context,
                      Page page) {
        this.testId = testId; // пригодится при генерации имён файлов с артефактами
        this.config = config; // храним ссылку, чтобы страницы знали настройки
        this.playwright = playwright;
        this.browser = browser;
        this.context = context; // каждый тест работает в своём браузерном контексте
        this.page = page; // готовая вкладка, с которой взаимодействуют Page Object'ы
    }

    public String testId() {
        return testId;
    }

    public Page page() {
        return page;
    }

    public BrowserContext context() {
        return context;
    }

    public FrameworkConfig config() {
        return config;
    }

    public void startTracingIfEnabled() {
        if (config.traceEnabled() && !tracingStarted) {
            context.tracing().start(new Tracing.StartOptions()
                    .setSnapshots(true)
                    .setSources(true)
                    .setScreenshots(true));
            tracingStarted = true;
        }
    }

    public Optional<Path> exportTraceIfEnabled() {
        if (!config.traceEnabled() || !tracingStarted) {
            return Optional.empty();
        }
        Path tracePath = FileSystemSupport.buildArtifactPath(config.traceDir(), testId + "-trace-" + Instant.now().toEpochMilli(), ".zip"); // имя файла завязано на тест
        context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
        tracingStarted = false;
        return Optional.of(tracePath);
    }

    public void stopTracingSilently() {
        if (tracingStarted) {
            context.tracing().stop();
            tracingStarted = false;
        }
    }

    public byte[] captureScreenshot() {
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(true)); // fullPage удобно для аналитики багов
    }

    public Path persistScreenshot(byte[] screenshotBytes, String suffix) {
        Path target = FileSystemSupport.buildArtifactPath(
                config.screenshotsDir(),
                testId + "-" + suffix + "-" + Instant.now().toEpochMilli(),
                ".png"
        );
        try {
            java.nio.file.Files.write(target, screenshotBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to store screenshot to " + target, e);
        }
        return target;
    }

    public Optional<Path> closeAndCollectVideo(boolean persist, String suffix) {
        Video video = page.video(); // Playwright возвращает объект записи, если видео включено
        page.close(); // закрываем вкладку перед сбором артефактов
        if (video != null && config.videoEnabled()) {
            if (!persist) {
                video.delete();
                return Optional.empty();
            }
            Path target = FileSystemSupport.buildArtifactPath(
                    config.videoDir(),
                    testId + "-" + suffix + "-" + Instant.now().toEpochMilli(),
                    ".webm"
            );
            video.saveAs(target);
            video.delete();
            return Optional.of(target);
        }
        return Optional.empty();
    }

    @Override
    public void close() {
        try {
            context.close();
        } finally {
            try {
                browser.close();
            } finally {
                playwright.close();
            }
        }
    }
}

