package com.example.framework.runners;

import com.example.framework.tags.TestTags;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Release suite runner - full regression including slow tests, excluding unstable scenarios.
 */
@Suite
@SelectPackages("com.example.framework.tests")
@IncludeTags({TestTags.SMOKE, TestTags.REGRESSION, TestTags.SLOW})
@ExcludeTags(TestTags.FLAKY)
public class ReleaseRunner {
}

