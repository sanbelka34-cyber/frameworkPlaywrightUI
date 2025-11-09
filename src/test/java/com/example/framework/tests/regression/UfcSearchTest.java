package com.example.framework.tests.regression;

import com.example.framework.core.BaseTest;
import com.example.framework.pages.UfcHomePage;
import com.example.framework.tags.annotations.Regression;
import com.example.framework.tags.annotations.Slow;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * UFC search panel behaviour validation.
 */
@Epic("UFC portal")
@Feature("Search panel")
@Execution(ExecutionMode.SAME_THREAD)
class UfcSearchTest extends BaseTest {

    @Test
    @Regression
    @Slow
    @DisplayName("Autocomplete supports selecting first and last results sequentially")
    @Story("Search suggestions list responds to user selection order")
    void shouldSelectFirstAndLastSuggestion() {
        UfcHomePage ufcHomePage = new UfcHomePage(session().page(), config()).open();

        ufcHomePage.openSearchPanel()
                .focusSearchInput()
                .selectFirstSuggestion();

        ufcHomePage.open()
                .openSearchPanel()
                .focusSearchInput()
                .selectLastSuggestion();
    }
}

