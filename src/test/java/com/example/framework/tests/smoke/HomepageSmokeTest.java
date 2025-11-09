package com.example.framework.tests.smoke;

import com.example.framework.core.BaseTest;
import com.example.framework.pages.DocsPage;
import com.example.framework.pages.HomePage;
import com.example.framework.tags.annotations.Regression;
import com.example.framework.tags.annotations.Smoke;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke coverage focusing on the primary user journey.
 */
@Epic("Navigation")
class HomepageSmokeTest extends BaseTest {

    @Test
    @Smoke
    @Regression
    @DisplayName("Critical path: users can navigate to docs from the homepage")
    @Story("Essential documentation access")
    void shouldOpenDocsFromNavigation() {
        HomePage homePage = new HomePage(session().page(), config());
        DocsPage docsPage = homePage
                .open()
                .expectHeroVisible()
                .openDocs();

        docsPage.expectOnDocsLanding()
                .expectHeadingContains("Playwright");
    }
}

