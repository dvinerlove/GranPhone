package com.bald.uriah.baldphone.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PermissionContractTest {
    private static final int[] ATOMS = {
            PermissionContract.PERMISSION_ATOM_WRITE_SETTINGS,
            PermissionContract.PERMISSION_ATOM_DEFAULT_DIALER,
            PermissionContract.PERMISSION_ATOM_READ_CONTACTS,
            PermissionContract.PERMISSION_ATOM_WRITE_CONTACTS,
            PermissionContract.PERMISSION_ATOM_CALL_PHONE,
            PermissionContract.PERMISSION_ATOM_WRITE_CALL_LOG,
            PermissionContract.PERMISSION_ATOM_CAMERA,
            PermissionContract.PERMISSION_ATOM_WRITE_EXTERNAL_STORAGE,
            PermissionContract.PERMISSION_ATOM_NOTIFICATION_LISTENER,
            PermissionContract.PERMISSION_ATOM_REQUEST_INSTALL_PACKAGES,
            PermissionContract.PERMISSION_ATOM_READ_CALL_LOG,
            PermissionContract.PERMISSION_ATOM_READ_PHONE_STATE,
            PermissionContract.PERMISSION_ATOM_SYSTEM_ALERT_WINDOW,
            PermissionContract.PERMISSION_ATOM_POST_NOTIFICATIONS,
            PermissionContract.PERMISSION_ATOM_EXACT_ALARM,
            PermissionContract.PERMISSION_ATOM_MEDIA_IMAGES,
            PermissionContract.PERMISSION_ATOM_MEDIA_VIDEO
    };

    @Test
    public void atomsAreSingleUniqueBits() {
        int seen = 0;
        for (final int atom : ATOMS) {
            assertEquals(1, Integer.bitCount(atom));
            assertEquals(0, seen & atom);
            seen |= atom;
        }
    }

    private static int legacyTelephonyBase() {
        return PermissionContract.PERMISSION_ATOM_READ_CONTACTS
                | PermissionContract.PERMISSION_ATOM_WRITE_CONTACTS
                | PermissionContract.PERMISSION_ATOM_CALL_PHONE
                | PermissionContract.PERMISSION_ATOM_READ_CALL_LOG
                | PermissionContract.PERMISSION_ATOM_WRITE_CALL_LOG;
    }

    @Test
    public void compositesAreExplicitUnions() {
        final int legacyTelephonyBase = legacyTelephonyBase();
        assertEquals(legacyTelephonyBase, PermissionContract.PERMISSION_READ_CONTACTS);
        assertEquals(legacyTelephonyBase, PermissionContract.PERMISSION_WRITE_CONTACTS);
        assertEquals(legacyTelephonyBase, PermissionContract.PERMISSION_READ_CALL_LOG);
        assertEquals(legacyTelephonyBase, PermissionContract.PERMISSION_WRITE_CALL_LOG);
        assertEquals(legacyTelephonyBase
                        | PermissionContract.PERMISSION_ATOM_READ_PHONE_STATE
                        | PermissionContract.PERMISSION_ATOM_POST_NOTIFICATIONS,
                PermissionContract.PERMISSION_CALL_PHONE);
        assertEquals(PermissionContract.PERMISSION_ATOM_NOTIFICATION_LISTENER
                        | PermissionContract.PERMISSION_ATOM_WRITE_SETTINGS,
                PermissionContract.PERMISSION_NOTIFICATION_LISTENER);
    }

    @Test
    public void wave3aCallerMasksKeepLegacyEffectiveAtoms() {
        assertEquals(legacyTelephonyBase()
                        | PermissionContract.PERMISSION_ATOM_READ_PHONE_STATE
                        | PermissionContract.PERMISSION_ATOM_POST_NOTIFICATIONS,
                PermissionContract.PERMISSION_READ_CONTACTS | PermissionContract.PERMISSION_CALL_PHONE);
        assertEquals(legacyTelephonyBase() | PermissionContract.PERMISSION_ATOM_MEDIA_IMAGES,
                PermissionContract.PERMISSION_WRITE_CONTACTS
                        | PermissionContract.PERMISSION_READ_CONTACTS
                        | PermissionContract.PERMISSION_MEDIA_IMAGES);
        assertEquals(legacyTelephonyBase(),
                PermissionContract.PERMISSION_WRITE_CALL_LOG | PermissionContract.PERMISSION_READ_CONTACTS);
    }

    @Test
    public void legacyTelephonyCompositesDoNotEnforceTheDialerRole() {
        assertEquals(0, PermissionContract.PERMISSION_CALL_PHONE & PermissionContract.PERMISSION_ATOM_DEFAULT_DIALER);
        assertEquals(0, PermissionContract.PERMISSION_READ_CONTACTS & PermissionContract.PERMISSION_ATOM_DEFAULT_DIALER);
        assertEquals(PermissionContract.PERMISSION_ATOM_DEFAULT_DIALER,
                PermissionContract.PERMISSION_DEFAULT_PHONE_HANDLER);
    }

    @Test
    public void allIsGrantableAtomsWithoutTheDialerRole() {
        int expected = 0;
        for (final int atom : ATOMS)
            if (atom != PermissionContract.PERMISSION_ATOM_DEFAULT_DIALER)
                expected |= atom;
        assertEquals(expected, PermissionContract.PERMISSION_ALL);
        assertEquals(0, PermissionContract.PERMISSION_ALL & PermissionContract.PERMISSION_ATOM_DEFAULT_DIALER);
        assertEquals(PermissionContract.PERMISSION_ALL,
                PermissionContract.normalizeRequiredPermissions(-1));
        assertTrue(PermissionContract.hasAtom(PermissionContract.PERMISSION_CALL_PHONE,
                PermissionContract.PERMISSION_ATOM_CALL_PHONE));
    }
}
