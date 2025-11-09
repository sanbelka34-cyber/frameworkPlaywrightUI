package com.example.framework.pages;

import com.example.framework.config.FrameworkConfig;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.NavigateOptions;
import com.microsoft.playwright.options.WaitUntilState;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;

/**
 * Page object encapsulating interactions with https://ufc.ru/.
 */
public class UfcHomePage extends BasePage<UfcHomePage> {

    private static final String UFC_HOME_URL = "https://ufc.ru/";
    private static final String SEARCH_TOGGLE = "button[aria-label=\"Search panel toggle\"]";
    private static final String SEARCH_INPUT = "#yxt-SearchBar-input--search-bar";
    private static final String SUGGESTION_ITEMS = "li.yxt-AutoComplete-option--item";

    public UfcHomePage(Page page, FrameworkConfig config) {
        super(page, config);
    }

    @Override
    protected UfcHomePage self() {
        return this;
    }

    @Step("Open UFC home page")
    public UfcHomePage open() {
        page.navigate(UFC_HOME_URL, new NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        return this;
    }

    @Step("Toggle the search panel")
    public UfcHomePage openSearchPanel() {
        page.click(SEARCH_TOGGLE);
        return this;
    }

    @Step("Focus the search input")
    public UfcHomePage focusSearchInput() {
        Locator input = page.locator(SEARCH_INPUT);
        input.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        input.click();
        return this;
    }

    @Step("Select the first suggestion")
    public UfcHomePage selectFirstSuggestion() {
        waitForSuggestions();
        Locator first = suggestions().first();
        first.click();
        page.waitForTimeout(1000);
        return this;
    }

    @Step("Select the last suggestion")
    public UfcHomePage selectLastSuggestion() {
        waitForSuggestions();
        Locator last = suggestions().last();
        last.click();
        page.waitForTimeout(1000);
        return this;
    }

    private void waitForSuggestions() {
        suggestions().first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    private Locator suggestions() {
        return page.locator(SUGGESTION_ITEMS);
    }
}

