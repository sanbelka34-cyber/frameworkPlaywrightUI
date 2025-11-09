package com.example.framework.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads configuration from properties file, environment variables, and JVM system properties.
 */
public final class ConfigurationManager {

    private static final String CONFIG_RESOURCE = "config/framework.properties";
    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();

    private static final FrameworkConfig CONFIG = load();

    private ConfigurationManager() {
    }

    public static FrameworkConfig configuration() {
        return CONFIG;
    }

    private static FrameworkConfig load() {
        Properties properties = new Properties();
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load configuration from " + CONFIG_RESOURCE, e);
        }

        String baseUrl = resolveString(ConfigKeys.BASE_URL, properties, "https://playwright.dev");
        String browser = resolveString(ConfigKeys.BROWSER, properties, "chromium");
        boolean headless = resolveBoolean(ConfigKeys.HEADLESS, properties, true);
        int slowMo = resolveInteger(ConfigKeys.SLOW_MO, properties, 0);
        int timeout = resolveInteger(ConfigKeys.TIMEOUT_MS, properties, 30_000);
        boolean videoEnabled = resolveBoolean(ConfigKeys.VIDEO_ENABLED, properties, true);
        Path videoDir = resolvePath(ConfigKeys.VIDEO_FOLDER, properties, "target/videos");
        boolean traceEnabled = resolveBoolean(ConfigKeys.TRACE_ENABLED, properties, true);
        Path traceDir = resolvePath(ConfigKeys.TRACE_FOLDER, properties, "target/traces");
        Path screenshotsDir = resolvePath(ConfigKeys.SCREENSHOTS_FOLDER, properties, "target/screenshots");
        Path downloadsDir = resolvePath(ConfigKeys.DOWNLOADS_FOLDER, properties, "target/downloads");
        int parallelism = resolveInteger(ConfigKeys.PARALLELISM, properties, Runtime.getRuntime().availableProcessors());

        return new FrameworkConfig(
                baseUrl,
                browser,
                headless,
                slowMo,
                timeout,
                videoEnabled,
                videoDir,
                traceEnabled,
                traceDir,
                screenshotsDir,
                downloadsDir,
                parallelism
        );
    }

    private static String resolveString(String key, Properties properties, String defaultValue) {
        return resolve(key, properties, defaultValue);
    }

    private static boolean resolveBoolean(String key, Properties properties, boolean defaultValue) {
        return Boolean.parseBoolean(resolve(key, properties, Boolean.toString(defaultValue)));
    }

    private static int resolveInteger(String key, Properties properties, int defaultValue) {
        String value = resolve(key, properties, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value '" + value + "' for config key: " + key, e);
        }
    }

    private static Path resolvePath(String key, Properties properties, String defaultValue) {
        String value = resolve(key, properties, defaultValue);
        return Paths.get(value).toAbsolutePath().normalize();
    }

    private static String resolve(String key, Properties properties, String defaultValue) {
        String system = System.getProperty(key);
        if (isNotBlank(system)) {
            return system;
        }

        String env = System.getenv(toEnvKey(key));
        if (isNotBlank(env)) {
            return env;
        }

        String dotenvValue = DOTENV.get(key);
        if (isNotBlank(dotenvValue)) {
            return dotenvValue;
        }

        String property = properties.getProperty(key);
        if (isNotBlank(property)) {
            return property;
        }

        return Objects.requireNonNullElse(defaultValue, "");
    }

    private static String toEnvKey(String key) {
        return key.replace('.', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

