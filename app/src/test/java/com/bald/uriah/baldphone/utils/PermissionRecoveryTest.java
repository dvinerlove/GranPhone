package com.bald.uriah.baldphone.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PermissionRecoveryTest {
    @Test
    public void denialWithRationaleStillUsesRuntimeRequest() {
        final PermissionRecovery recovery = new PermissionRecovery();
        recovery.onRequestResult("camera", false, true);

        assertFalse(recovery.shouldOpenAppSettings(
                new String[]{"camera"}, new boolean[]{false}, new boolean[]{true}));
    }

    @Test
    public void denialWithoutRationaleOpensSettingsOnlyAfterObservedResult() {
        final PermissionRecovery recovery = new PermissionRecovery();
        assertFalse(recovery.shouldOpenAppSettings(
                new String[]{"camera"}, new boolean[]{false}, new boolean[]{false}));

        recovery.onRequestResult("camera", false, false);
        assertTrue(recovery.shouldOpenAppSettings(
                new String[]{"camera"}, new boolean[]{false}, new boolean[]{false}));
    }

    @Test
    public void mixedCardsKeepRuntimeRequestUntilEveryMissingPermissionIsDeadEnded() {
        final PermissionRecovery recovery = new PermissionRecovery();
        recovery.onRequestResult("images", false, false);

        assertFalse(recovery.shouldOpenAppSettings(
                new String[]{"images", "video"}, new boolean[]{false, false}, new boolean[]{false, true}));
        assertTrue(recovery.shouldOpenAppSettings(
                new String[]{"images", "video"}, new boolean[]{false, true}, new boolean[]{false, false}));
    }

    @Test
    public void grantClearsPermanentDenialMarker() {
        final PermissionRecovery recovery = new PermissionRecovery();
        recovery.onRequestResult("camera", false, false);
        recovery.onRequestResult("camera", true, false);

        assertFalse(recovery.shouldOpenAppSettings(
                new String[]{"camera"}, new boolean[]{false}, new boolean[]{false}));
    }
}
