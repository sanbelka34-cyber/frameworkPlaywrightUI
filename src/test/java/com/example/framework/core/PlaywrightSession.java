package com.example.framework.core;

import com.example.framework.config.FrameworkConfig;
import com.example.framework.core.support.FileSystemSupport;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.Video;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents an isolated Playwright session for a single test execution.
 */
public final class PlaywrightSession implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PlaywrightSession.class);

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
            LOG.info("Запускаем Playwright-трейс для {}", testId);
            context.tracing().start(new Tracing.StartOptions()
                    .setSnapshots(true)
                    .setSources(true)
                    .setScreenshots(true));
            tracingStarted = true;
        } else {
            LOG.info("Трейс для {} не запущен (включен={} ужеНачат={})", testId, config.traceEnabled(), tracingStarted);
        }
    }

    public Optional<Path> exportTraceIfEnabled() {
        if (!config.traceEnabled() || !tracingStarted) {
            LOG.info("Экспорт трейса для {} пропущен (включен={} ужеНачат={})", testId, config.traceEnabled(), tracingStarted);
            return Optional.empty();
        }
        Path tracePath = FileSystemSupport.buildArtifactPath(config.traceDir(), testId + "-trace-" + Instant.now().toEpochMilli(), ".zip"); // имя файла завязано на тест
        context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
        tracingStarted = false;
        LOG.info("Трейс для {} сохранен в {}", testId, tracePath);
        return Optional.of(tracePath);
    }

    public void stopTracingSilently() {
        if (tracingStarted) {
            LOG.info("Останавливаем трейс для {} без экспорта", testId);
            context.tracing().stop();
            tracingStarted = false;
        } else {
            LOG.info("Трейс для {} уже остановлен", testId);
        }
    }

    public byte[] captureScreenshot() {
        LOG.info("Делаем скриншот для {}", testId);
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
        LOG.info("Скриншот для {} сохранен в {}", testId, target);
        return target;
    }

    public Optional<Path> closeAndCollectVideo(boolean persist, String suffix) {
        Video video = page.video(); // Playwright возвращает объект записи, если видео включено
        page.close(); // закрываем вкладку перед сбором артефактов
        if (video != null && config.videoEnabled()) {
            if (!persist) {
                LOG.info("Видео для {} удалено по запросу", testId);
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
            LOG.info("Видео для {} сохранено в {}", testId, target);
            return Optional.of(target);
        }
        LOG.info("Видео для {} отсутствует (видеоВключено={}, видеоПолучено={})",
                testId, config.videoEnabled(), video != null);
        return Optional.empty();
    }

    @Override
    public void close() {
        LOG.info("Закрываем Playwright-сессию {}", testId);
        try {
            context.close();
        } finally {
            try {
                browser.close();
            } finally {
                playwright.close();
                LOG.info("Playwright-сессия {} закрыта", testId);
            }
        }
    }
}

