package com.example.framework.core;

import com.example.framework.config.ConfigurationManager;
import com.example.framework.config.FrameworkConfig;
import com.example.framework.core.support.FileSystemSupport;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.Playwright;

import java.util.Locale;

/**
 * Builds configured Playwright sessions for tests.
 */
public final class PlaywrightFactory {

    private PlaywrightFactory() {
    }

    public static PlaywrightSession newSession(String testId) {
        FrameworkConfig config = ConfigurationManager.configuration(); // берём все параметры запуска тестов

        FileSystemSupport.ensureDirectory(config.videoDir()); // гарантируем, что папка для видео существует
        FileSystemSupport.ensureDirectory(config.traceDir()); // то же для трейсов
        FileSystemSupport.ensureDirectory(config.screenshotsDir()); // и для скриншотов
        FileSystemSupport.ensureDirectory(config.downloadsDir()); // и для загрузок

        Playwright playwright = Playwright.create(); // создаём основной Playwright-движок
        Browser browser = selectBrowser(playwright, config.browser())
                .launch(new LaunchOptions()
                        .setHeadless(config.headless()) // управляем режимом headless через конфиг
                        .setSlowMo(config.slowMo())); // замедление действий удобно при отладке

        NewContextOptions contextOptions = new NewContextOptions()
                .setBaseURL(config.baseUrl()) // чтобы `page.navigate()` мог использовать относительные пути
                .setViewportSize(1280, 720) // фиксируем типичный размер окна
                .setAcceptDownloads(true) // разрешаем скачивания, иначе Playwright будет блокировать
                .setLocale(Locale.getDefault().toLanguageTag()) // используем текущую локаль машины
                .setIgnoreHTTPSErrors(true); // избегаем падений на self-signed сертификатах

        if (config.videoEnabled()) {
            contextOptions.setRecordVideoDir(config.videoDir()); // включаем запись видео в папку из конфига
            contextOptions.setRecordVideoSize(1280, 720); // размер ролика совпадает с viewport
        }

        Browser.NewContextOptions finalOptions = contextOptions;
        BrowserContextPair contextPair = BrowserContextPair.create(browser, finalOptions); // обёртка возвращает контекст и страницу

        contextPair.context.setDefaultTimeout(config.timeoutMs()); // единый таймаут для всех действий
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
            case "firefox" -> playwright.firefox(); // стандартное соответствие названию браузера
            case "webkit" -> playwright.webkit();
            case "chromium", "", "chrome" -> playwright.chromium(); // default: chromium, если ничего не указано
            default -> throw new IllegalArgumentException("Unsupported browser type: " + browserName);
        };
    }

    private record BrowserContextPair(com.microsoft.playwright.BrowserContext context,
                                      com.microsoft.playwright.Page page) {

        private static BrowserContextPair create(Browser browser, NewContextOptions options) {
            var context = browser.newContext(options); // создаём изолированный контекст на каждый тест
            return new BrowserContextPair(context, context.newPage()); // сразу открываем вкладку, чтобы использовать её дальше
        }
    }
}

