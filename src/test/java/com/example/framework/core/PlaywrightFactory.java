package com.example.framework.core;

import com.example.framework.config.ConfigurationManager;
import com.example.framework.config.FrameworkConfig;
import com.example.framework.core.support.FileSystemSupport;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.Playwright;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Builds configured Playwright sessions for tests.
 */
public final class PlaywrightFactory {

    private PlaywrightFactory() {
    }

    public static PlaywrightSession newSession(String testId) {
        FrameworkConfig config = ConfigurationManager.configuration();

        FileSystemSupport.ensureDirectory(config.videoDir());
        FileSystemSupport.ensureDirectory(config.traceDir());
        FileSystemSupport.ensureDirectory(config.screenshotsDir());
        FileSystemSupport.ensureDirectory(config.downloadsDir());

        Playwright playwright = Playwright.create();
        Browser browser = selectBrowser(playwright, config.browser())
                .launch(new LaunchOptions()
                        .setHeadless(config.headless())
                        .setSlowMo(config.slowMo()));

        NewContextOptions contextOptions = new NewContextOptions()
                .setBaseURL(config.baseUrl())
                .setViewportSize(1280, 720)
                .setAcceptDownloads(true)
                .setLocale(Locale.getDefault().toLanguageTag())
                .setIgnoreHTTPSErrors(true);

        if (config.videoEnabled()) {
            contextOptions.setRecordVideoDir(config.videoDir());
            contextOptions.setRecordVideoSize(1280, 720);
        }

        Browser.NewContextOptions finalOptions = contextOptions;
        BrowserContextPair contextPair = BrowserContextPair.create(browser, finalOptions);

        contextPair.context.setDefaultTimeout(config.timeoutMs());
        contextPair.page.setDefaultTimeout(config.timeoutMs());

        return new PlaywrightSession(
                testId,
                config,
                playwright,
                browser,
                contextPair.context,
                contextPair.page
        );
    }

    private static BrowserType selectBrowser(Playwright playwright, String browserName) {
        String normalized = browserName == null ? "" : browserName.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            case "chromium", "", "chrome" -> playwright.chromium();
            default -> throw new IllegalArgumentException("Unsupported browser type: " + browserName);
        };
    }

    private record BrowserContextPair(com.microsoft.playwright.BrowserContext context,
                                      com.microsoft.playwright.Page page) {

        private static BrowserContextPair create(Browser browser, NewContextOptions options) {
            var context = browser.newContext(options);
            return new BrowserContextPair(context, context.newPage());
        }
    }
}

