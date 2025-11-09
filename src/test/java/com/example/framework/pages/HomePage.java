package com.example.framework.pages;

import com.example.framework.config.FrameworkConfig;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.GetByRoleOptions;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

/**
 * Page object representing the marketing homepage.
 */
public class HomePage extends BasePage<HomePage> {

    public HomePage(Page page, FrameworkConfig config) {
        super(page, config);
    }

    @Override
    protected HomePage self() {
        return this;
    }

    @Step("Open Playwright home page")
    public HomePage open() {
        return openHome();
    }

    @Step("Validate hero section is visible")
    public HomePage expectHeroVisible() {
        expectTitleContains("Playwright");
        return this;
    }

    @Step("Navigate to documentation")
    public DocsPage openDocs() {
        page.getByRole(AriaRole.LINK, new GetByRoleOptions().setName("Docs")).click();
        return new DocsPage(page, config);
    }
}

