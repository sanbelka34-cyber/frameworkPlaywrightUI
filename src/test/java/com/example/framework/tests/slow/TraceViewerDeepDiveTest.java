package com.example.framework.tests.slow;

import com.example.framework.core.BaseTest;
import com.example.framework.pages.DocsPage;
import com.example.framework.pages.HomePage;
import com.example.framework.tags.annotations.Regression;
import com.example.framework.tags.annotations.Slow;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Longer-running scenario that exercises additional navigation depth.
 */
@Epic("Advanced workflows")
class TraceViewerDeepDiveTest extends BaseTest {

    @Test
    @Regression
    @Slow
    @DisplayName("Slow: deep dive into trace viewer documentation")
    @Story("Detailed instrumentation setup")
    void deepDiveIntoTraceViewer() {
        DocsPage docsPage = new HomePage(session().page(), config())
                .open()
                .openDocs();

        docsPage.expectOnDocsLanding()
                .expectHeadingNotBlank();

        // Simulate additional diagnostic steps that typically extend execution time.
        session().page().waitForTimeout(1500);
    }
}

