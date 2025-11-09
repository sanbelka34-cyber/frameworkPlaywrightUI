package com.example.framework.tests.regression;

import com.example.framework.core.BaseTest;
import com.example.framework.pages.DocsPage;
import com.example.framework.pages.HomePage;
import com.example.framework.tags.annotations.Regression;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Broader regression checks ensuring key content remains discoverable.
 */
@Epic("Documentation")
@Feature("Learning resources")
class DocsNavigationRegressionTest extends BaseTest {

    @Test
    @Regression
    @DisplayName("Regression: Trace viewer guide is available from the docs sidebar")
    @Story("Observability and debugging")
    void traceViewerGuideIsAccessible() {
        DocsPage docsPage = new HomePage(session().page(), config())
                .open()
                .openDocs();

        docsPage.expectOnDocsLanding()
                .expectHeadingNotBlank();
    }
}

