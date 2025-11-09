package com.example.framework.pages;

import com.example.framework.config.FrameworkConfig;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.GetByRoleOptions;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;

/**
 * Page object modelling the documentation experience.
 */
public class DocsPage extends BasePage<DocsPage> {

    public DocsPage(Page page, FrameworkConfig config) {
        super(page, config);
    }

    @Override
    protected DocsPage self() {
        return this;
    }

    @Step("Expect documentation landing page to be loaded")
    public DocsPage expectOnDocsLanding() {
        page.waitForURL("**/docs/**");
        expectTitleContains("Playwright");
        return this;
    }

    @Step("Open sidebar topic: {topic}")
    public DocsPage openSidebarTopic(String topic) {
        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName(topic)).first().click();
        page.waitForLoadState();
        return this;
    }

    @Step("Navigate directly to docs topic: {relativePath}")
    public DocsPage openTopic(String relativePath) {
        String sanitized = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        page.navigate(config.baseUrl() + "/docs/" + sanitized);
        page.waitForLoadState();
        return this;
    }

    @Step("Expect sidebar link visible: {linkText}")
    public DocsPage expectSidebarLinkVisible(String linkText) {
        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName(linkText)).first().waitFor();
        return this;
    }

    @Step("Expect heading to contain: {expected}")
    public DocsPage expectHeadingContains(String expected) {
        String heading = page.getByRole(AriaRole.HEADING, new GetByRoleOptions().setLevel(1)).innerText().trim();
        Assertions.assertTrue(
                heading.toLowerCase().contains(expected.toLowerCase()),
                () -> "Expected heading to contain [%s] but was [%s]".formatted(expected, heading)
        );
        return this;
    }

    @Step("Expect heading is not empty")
    public DocsPage expectHeadingNotBlank() {
        String heading = page.getByRole(AriaRole.HEADING, new GetByRoleOptions().setLevel(1)).innerText().trim();
        Assertions.assertFalse(heading.isBlank(), "Expected heading to be non-empty");
        return this;
    }
}

