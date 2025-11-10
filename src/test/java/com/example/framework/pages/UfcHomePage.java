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
        super(page, config); // пробрасываем Playwright Page и настройки в базовый конструктор
    }

    @Override
    protected UfcHomePage self() {
        return this;
    }

    @Step("Open UFC home page")
    public UfcHomePage open() {
        page.navigate(UFC_HOME_URL, new NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED)); // ждём, пока основное содержимое загрузится
        return this;
    }

    @Step("Toggle the search panel")
    public UfcHomePage openSearchPanel() {
        page.click(SEARCH_TOGGLE); // раскрываем скрытую панель поиска
        return this;
    }

    @Step("Focus the search input")
    public UfcHomePage focusSearchInput() {
        Locator input = page.locator(SEARCH_INPUT);
        input.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)); // дожидаемся появления поля
        input.click(); // и ставим в него фокус
        return this;
    }

    @Step("Select the first suggestion")
    public UfcSearchPage selectFirstSuggestion() {
        waitForSuggestions();
        Locator first = suggestions().first(); // Playwright возвращает все подходящие элементы
        first.click();
        page.waitForTimeout(1000); // даём сайту время обновить результаты
        return new UfcSearchPage(page, config);
    }

    @Step("Select the last suggestion")
    public UfcHomePage selectLastSuggestion() {
        waitForSuggestions();
        Locator last = suggestions().last(); // выбираем другой элемент, чтобы убедиться, что список реагирует
        last.click();
        page.waitForTimeout(1000);
        return this;
    }

    private void waitForSuggestions() {
        suggestions().first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)); // ждём, пока появятся подсказки
    }

    private Locator suggestions() {
        return page.locator(SUGGESTION_ITEMS); // селектор для всех элементов подсказок
    }
}

