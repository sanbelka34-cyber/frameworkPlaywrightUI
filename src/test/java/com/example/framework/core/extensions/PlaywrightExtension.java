package com.example.framework.core.extensions;

import com.example.framework.core.PlaywrightFactory;
import com.example.framework.core.PlaywrightSession;
import com.example.framework.core.support.FileSystemSupport;
import com.example.framework.tags.annotations.Flaky;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * JUnit 5 extension that manages Playwright lifecycle and Allure attachments.
 */
public class PlaywrightExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver, TestWatcher {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(PlaywrightExtension.class);
    @Override
    public void beforeEach(ExtensionContext context) {
        String testId = FileSystemSupport.sanitizeFileName(context.getUniqueId()); // создаём аккуратный идентификатор, чтобы назвать файлы
        PlaywrightSession session = PlaywrightFactory.newSession(testId); // одна сессия на тест
        session.startTracingIfEnabled(); // сразу включаем трейс, если это разрешено конфигом
        context.getStore(NAMESPACE).put(sessionKey(context), session); // кладём сессию в стор, чтобы доставать позже
        context.getStore(NAMESPACE).put(artifactsKey(context), new Artifacts()); // структура для будущих вложений Allure

        // Automatically navigate to base URL to make tests more declarative.
        session.page().navigate(session.config().baseUrl(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED)); // заранее открываем базовый адрес, чтобы тесты были лаконичнее
    }

    @Override
    public void afterEach(ExtensionContext context) {
        PlaywrightSession session = getSession(context);
        if (session == null) {
            return;
        }

        Artifacts artifacts = getArtifacts(context);
        Optional<Path> videoPath = session.closeAndCollectVideo(artifacts.failed, artifacts.failed ? "failure" : "success"); // при успехе видео можно удалять

        if (artifacts.failed) {
            videoPath.ifPresent(path -> attachFile("Failure video", "video/webm", ".webm", path)); // прикладываем видео только когда упало
        }

        session.close(); // закрываем контекст, браузер и Playwright
        context.getStore(NAMESPACE).remove(sessionKey(context)); // чистим стор, чтобы не было утечек
        context.getStore(NAMESPACE).remove(artifactsKey(context));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type.equals(PlaywrightSession.class)
                || type.equals(com.microsoft.playwright.Page.class) // можно просить напрямую Playwright `Page`
                || type.equals(com.microsoft.playwright.BrowserContext.class); // или `BrowserContext`
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        PlaywrightSession session = getSession(extensionContext);
        Class<?> type = parameterContext.getParameter().getType();
        if (type.equals(PlaywrightSession.class)) {
            return session;
        }
        if (type.equals(com.microsoft.playwright.Page.class)) {
            return session.page(); // прокидываем текущую вкладку
        }
        if (type.equals(com.microsoft.playwright.BrowserContext.class)) {
            return session.context(); // или весь контекст, если нужно несколько вкладок
        }
        throw new IllegalStateException("Unsupported parameter type: " + type);
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        PlaywrightSession session = getSession(context);
        if (session != null) {
            session.stopTracingSilently();
        }
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        // Clean tracing to avoid leaking resources.
        PlaywrightSession session = getSession(context);
        if (session != null) {
            session.stopTracingSilently();
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        PlaywrightSession session = getSession(context);
        if (session == null) {
            return;
        }
        Artifacts artifacts = getArtifacts(context);
        artifacts.failed = true; // позже afterEach поймёт, что надо сохранять видео

        byte[] screenshot = session.captureScreenshot(); // делаем full-page скриншот
        Path storedScreenshot = session.persistScreenshot(screenshot, "failure"); // сохраняем на диск с понятным именем
        Allure.addAttachment("Failure screenshot", "image/png", new ByteArrayInputStream(screenshot), ".png"); // добавляем в отчёт Allure
        artifacts.screenshotPath = storedScreenshot;

        session.exportTraceIfEnabled().ifPresent(path -> {
            artifacts.tracePath = path; // пригодится для локального анализа
            attachFile("Playwright trace", "application/zip", ".zip", path); // выгружаем трейс в Allure
        });

        annotateFlakyIfNeeded(context);
    }

    private void annotateFlakyIfNeeded(ExtensionContext context) {
        if (context.getRequiredTestMethod().isAnnotationPresent(Flaky.class)
                || context.getRequiredTestClass().isAnnotationPresent(Flaky.class)) {
            Allure.label("flaky", "true");
        }
    }

    private PlaywrightSession getSession(ExtensionContext context) {
        return context.getStore(NAMESPACE).get(sessionKey(context), PlaywrightSession.class);
    }

    private Artifacts getArtifacts(ExtensionContext context) {
        return context.getStore(NAMESPACE).get(artifactsKey(context), Artifacts.class);
    }

    private void attachFile(String name, String mimeType, String extension, Path path) {
        try {
            Allure.addAttachment(name, mimeType, Files.newInputStream(path), extension);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to attach file " + path, e);
        }
    }

    private static final class Artifacts {
        private boolean failed;
        @SuppressWarnings("unused")
        private Path screenshotPath; // оставляем ссылку, если тесту нужно будет обработать файл вручную
        @SuppressWarnings("unused")
        private Path tracePath; // пригодится при отладке, если нужно будет прочитать путь из стора
    }

    private String sessionKey(ExtensionContext context) {
        return context.getUniqueId() + "-session";
    }

    private String artifactsKey(ExtensionContext context) {
        return context.getUniqueId() + "-artifacts";
    }
}

