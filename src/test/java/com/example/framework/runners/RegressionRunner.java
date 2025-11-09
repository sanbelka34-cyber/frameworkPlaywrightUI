package com.example.framework.runners;

import com.example.framework.tags.TestTags;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Regression suite runner - includes broad coverage while skipping slow scenarios for nightly builds.
 */
@Suite
@SelectPackages("com.example.framework.tests")
@IncludeTags({TestTags.REGRESSION})
@ExcludeTags({TestTags.SLOW})
public class RegressionRunner {
}

