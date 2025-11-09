package com.example.framework.core;

import com.example.framework.config.FrameworkConfig;
import com.example.framework.core.extensions.PlaywrightExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Base class for tests that exposes convenience accessors to the Playwright session.
 */
@ExtendWith(PlaywrightExtension.class)
public abstract class BaseTest {

    private PlaywrightSession session;

    @BeforeEach
    void captureSession(PlaywrightSession session) {
        this.session = session;
    }

    protected PlaywrightSession session() {
        return session;
    }

    protected FrameworkConfig config() {
        return session.config();
    }
}

