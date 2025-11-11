package com.example.framework.pages;

import com.example.framework.config.FrameworkConfig;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Base class for all page objects supplying convenience helpers and Allure-friendly steps.
 *
 * @param <T> concrete page type
 */
public abstract class BasePage<T extends BasePage<T>> {

    protected final Page page;
    protected final FrameworkConfig config;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected BasePage(Page page, FrameworkConfig config) {
        this.page = page; // общий Playwright Page для действий страницы
        this.config = config; // нужен для доступа к baseUrl и таймаутам
    }

    protected abstract T self();

    @Step("Open application base URL")
    public T openHome() {
        return step("Open application base URL", () -> {
            log.info("Переходим на базовый адрес {}", config.baseUrl());
            page.navigate(config.baseUrl(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            return self();
        });
    }

    @Step("Navigate to path: {path}")
    public T openPath(String path) {
        String stepName = "Navigate to path: " + path;
        return step(stepName, () -> {
            log.info("Открываем путь {}{}", config.baseUrl(), path);
            page.navigate(config.baseUrl() + path); // даёт возможность работать с относительными путями
            return self();
        });
    }

    @Step("Expect page title to contain: {expected}")
    public T expectTitleContains(String expected) {
        String stepName = "Expect page title to contain: " + expected;
        return step(stepName, () -> {
            String actual = page.title();
            log.info("Проверяем, что заголовок страницы содержит '{}'", expected);
            Assertions.assertTrue(actual.contains(expected),
                    () -> "Expected title to contain [%s] but was [%s]".formatted(expected, actual));
            return self();
        });
    }

    @Step("Wait for element {selector} to be visible")
    public T waitForVisible(String selector) {
        String stepName = "Wait for element " + selector + " to be visible";
        return step(stepName, () -> {
            log.info("Ожидаем появления элемента '{}'", selector);
            locator(selector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            return self();
        });
    }

    protected Locator locator(String selector) {
        return page.locator(selector); // базовый хелпер: все элементы ищем одинаково
    }

    protected T step(String name, Supplier<T> supplier) {
        return stepResult(name, supplier);
    }

    protected <R> R stepResult(String name, Supplier<R> supplier) {
        StepResult<R> result = new StepResult<>();
        Allure.step(name, () -> result.value = supplier.get());
        return result.value;
    }

    private static final class StepResult<R> {
        private R value;
    }
}

