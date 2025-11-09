package com.example.framework.tests.monitoring;

import com.example.framework.core.BaseTest;
import com.example.framework.pages.HomePage;
import com.example.framework.tags.annotations.Flaky;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Example of a test marked as flaky to ensure dashboards can track instability.
 */
@Epic("Quality monitoring")
class ExperimentalFlakyTest extends BaseTest {

    @Test
    @Flaky
    @DisplayName("Flaky: experimental navigation experiment")
    @Story("Observe randomised experiment behaviour")
    void experimentalFeatureToggle() {
        new HomePage(session().page(), config())
                .open()
                .expectHeroVisible();

        // Simulate an experiment that may behave differently; we keep the test stable while marking it flaky.
        double simulatedMetric = ThreadLocalRandom.current().nextDouble();
        org.junit.jupiter.api.Assertions.assertTrue(simulatedMetric >= 0.0 && simulatedMetric < 1.0,
                "Simulated metric should always be within the 0..1 range");
    }
}

