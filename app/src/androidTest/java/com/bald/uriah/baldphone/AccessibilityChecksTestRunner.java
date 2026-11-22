package com.bald.uriah.baldphone;

import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.runner.AndroidJUnitRunner;

public class AccessibilityChecksTestRunner extends AndroidJUnitRunner {
    {
        // wave 0: log violations instead of failing, until the baseline in
        // research/2026-11-22-granphone-ux-redesign/a11y-baseline.md is burned down.
        AccessibilityChecks.enable()
                .setRunChecksFromRootView(true)
                .setThrowExceptionForErrors(false);
    }
}
