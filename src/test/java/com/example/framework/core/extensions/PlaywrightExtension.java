package com.example.framework.core.extensions;

import com.example.framework.core.PlaywrightFactory;
import com.example.framework.core.PlaywrightSession;
import com.example.framework.core.support.FileSystemSupport;
import com.example.framework.tags.annotations.Flaky;
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
        String testId = FileSystemSupport.sanitizeFileName(context.getUniqueId());
        PlaywrightSession session = PlaywrightFactory.newSession(testId);
        session.startTracingIfEnabled();
        context.getStore(NAMESPACE).put(sessionKey(context), session);
        context.getStore(NAMESPACE).put(artifactsKey(context), new Artifacts());

        // Automatically navigate to base URL to make tests more declarative.
        session.page().navigate(session.config().baseUrl());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        PlaywrightSession session = getSession(context);
        if (session == null) {
            return;
        }

        Artifacts artifacts = getArtifacts(context);
        Optional<Path> videoPath = session.closeAndCollectVideo(artifacts.failed, artifacts.failed ? "failure" : "success");

        if (artifacts.failed) {
            videoPath.ifPresent(path -> attachFile("Failure video", "video/webm", ".webm", path));
        }

        session.close();
        context.getStore(NAMESPACE).remove(sessionKey(context));
        context.getStore(NAMESPACE).remove(artifactsKey(context));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type.equals(PlaywrightSession.class)
                || type.equals(com.microsoft.playwright.Page.class)
                || type.equals(com.microsoft.playwright.BrowserContext.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        PlaywrightSession session = getSession(extensionContext);
        Class<?> type = parameterContext.getParameter().getType();
        if (type.equals(PlaywrightSession.class)) {
            return session;
        }
        if (type.equals(com.microsoft.playwright.Page.class)) {
            return session.page();
        }
        if (type.equals(com.microsoft.playwright.BrowserContext.class)) {
            return session.context();
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
        artifacts.failed = true;

        byte[] screenshot = session.captureScreenshot();
        Path storedScreenshot = session.persistScreenshot(screenshot, "failure");
        Allure.addAttachment("Failure screenshot", "image/png", new ByteArrayInputStream(screenshot), ".png");
        artifacts.screenshotPath = storedScreenshot;

        session.exportTraceIfEnabled().ifPresent(path -> {
            artifacts.tracePath = path;
            attachFile("Playwright trace", "application/zip", ".zip", path);
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
        private Path screenshotPath;
        private Path tracePath;
    }

    private String sessionKey(ExtensionContext context) {
        return context.getUniqueId() + "-session";
    }

    private String artifactsKey(ExtensionContext context) {
        return context.getUniqueId() + "-artifacts";
    }
}

