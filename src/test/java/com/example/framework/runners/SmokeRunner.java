package com.example.framework.runners;

import com.example.framework.tags.TestTags;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Smoke suite runner - focused on critical paths. Intended for CI validation on every commit.
 */
@Suite
@SelectPackages("com.example.framework.tests")
@IncludeTags(TestTags.SMOKE)
public class SmokeRunner {
}

