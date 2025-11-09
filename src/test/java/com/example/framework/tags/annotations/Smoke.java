package com.example.framework.tags.annotations;

import com.example.framework.tags.TestTags;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test as part of the Smoke suite.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag(TestTags.SMOKE)
@Feature("Smoke")
@Severity(SeverityLevel.BLOCKER)
public @interface Smoke {
    String value() default "";
}

