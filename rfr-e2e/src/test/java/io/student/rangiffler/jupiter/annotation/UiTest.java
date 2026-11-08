package io.student.rangiffler.jupiter.annotation;

import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.jupiter.extension.BrowserOptions;
import io.student.rangiffler.jupiter.extension.BrowserTimeoutsExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("ui")
@UsePlaywright(BrowserOptions.class)
@ExtendWith(BrowserTimeoutsExtension.class)
@Timeout(30)
public @interface UiTest {
}
