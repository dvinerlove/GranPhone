/*
 * Copyright 2019 Uriah Shaul Mandel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bald.uriah.baldphone.utils;

/**
 * Permission bitmask contract: atoms are single, independent bits; the public
 * PERMISSION_* names used by activities are either one atom or an explicit union.
 * Bit positions reuse the old atomic positions where they already were unique.
 */
public final class PermissionContract {
    public static final int PERMISSION_NONE = 0;

    public static final int PERMISSION_ATOM_WRITE_SETTINGS = 0b1;
    public static final int PERMISSION_ATOM_DEFAULT_DIALER = 0b10;
    public static final int PERMISSION_ATOM_READ_CONTACTS = 0b100;
    public static final int PERMISSION_ATOM_WRITE_CONTACTS = 0b1000;
    public static final int PERMISSION_ATOM_CALL_PHONE = 0b10000;
    public static final int PERMISSION_ATOM_WRITE_CALL_LOG = 0b100000;
    public static final int PERMISSION_ATOM_CAMERA = 0b1000000;
    public static final int PERMISSION_ATOM_WRITE_EXTERNAL_STORAGE = 0b10000000;
    public static final int PERMISSION_ATOM_NOTIFICATION_LISTENER = 0b100000000;
    public static final int PERMISSION_ATOM_REQUEST_INSTALL_PACKAGES = 0b1000000000;
    public static final int PERMISSION_ATOM_READ_CALL_LOG = 0b10000000000;
    public static final int PERMISSION_ATOM_READ_PHONE_STATE = 0b100000000000;
    public static final int PERMISSION_ATOM_SYSTEM_ALERT_WINDOW = 0b1000000000000;
    public static final int PERMISSION_ATOM_POST_NOTIFICATIONS = 0b10000000000000;
    public static final int PERMISSION_ATOM_EXACT_ALARM = 0b100000000000000;
    public static final int PERMISSION_ATOM_MEDIA_IMAGES = 0b1000000000000000;
    public static final int PERMISSION_ATOM_MEDIA_VIDEO = 0b10000000000000000;

    public static final int PERMISSION_WRITE_SETTINGS = PERMISSION_ATOM_WRITE_SETTINGS;
    // The role bit is honest: it is required only by the full call-screen experience,
    // never by ordinary outgoing dialer/contact screens.
    public static final int PERMISSION_DEFAULT_PHONE_HANDLER = PERMISSION_ATOM_DEFAULT_DIALER;

    public static final int PERMISSION_READ_CONTACTS = PERMISSION_ATOM_READ_CONTACTS;
    public static final int PERMISSION_WRITE_CONTACTS = PERMISSION_ATOM_WRITE_CONTACTS;
    public static final int PERMISSION_READ_PHONE_STATE = PERMISSION_ATOM_READ_PHONE_STATE;
    public static final int PERMISSION_POST_NOTIFICATIONS = PERMISSION_ATOM_POST_NOTIFICATIONS;
    public static final int PERMISSION_CALL_PHONE = PERMISSION_ATOM_CALL_PHONE;
    public static final int PERMISSION_READ_CALL_LOG = PERMISSION_ATOM_READ_CALL_LOG;
    public static final int PERMISSION_WRITE_CALL_LOG = PERMISSION_ATOM_WRITE_CALL_LOG;
    public static final int PERMISSION_CAMERA = PERMISSION_ATOM_CAMERA;
    public static final int PERMISSION_WRITE_EXTERNAL_STORAGE = PERMISSION_ATOM_WRITE_EXTERNAL_STORAGE;
    public static final int PERMISSION_NOTIFICATION_LISTENER = PERMISSION_ATOM_NOTIFICATION_LISTENER;
    public static final int PERMISSION_REQUEST_INSTALL_PACKAGES = PERMISSION_ATOM_REQUEST_INSTALL_PACKAGES;
    public static final int PERMISSION_SYSTEM_ALERT_WINDOW = PERMISSION_ATOM_SYSTEM_ALERT_WINDOW;
    public static final int PERMISSION_EXACT_ALARM = PERMISSION_ATOM_EXACT_ALARM;
    public static final int PERMISSION_MEDIA_IMAGES = PERMISSION_ATOM_MEDIA_IMAGES;
    public static final int PERMISSION_MEDIA_VIDEO = PERMISSION_ATOM_MEDIA_VIDEO;

    // Full big-button incoming/ongoing call UX: the dialer role binds InCallService,
    // and notifications are the return-to-call/full-screen fallback on API 33+.
    public static final int PERMISSION_CALL_SCREEN =
            PERMISSION_ATOM_DEFAULT_DIALER | PERMISSION_ATOM_POST_NOTIFICATIONS;

    // «All permissions» means the complete product setup, including the dialer role.
    public static final int PERMISSION_ALL =
            PERMISSION_ATOM_WRITE_SETTINGS |
                    PERMISSION_ATOM_DEFAULT_DIALER |
                    PERMISSION_ATOM_READ_CONTACTS |
                    PERMISSION_ATOM_WRITE_CONTACTS |
                    PERMISSION_ATOM_CALL_PHONE |
                    PERMISSION_ATOM_WRITE_CALL_LOG |
                    PERMISSION_ATOM_CAMERA |
                    PERMISSION_ATOM_WRITE_EXTERNAL_STORAGE |
                    PERMISSION_ATOM_NOTIFICATION_LISTENER |
                    PERMISSION_ATOM_REQUEST_INSTALL_PACKAGES |
                    PERMISSION_ATOM_READ_CALL_LOG |
                    PERMISSION_ATOM_READ_PHONE_STATE |
                    PERMISSION_ATOM_SYSTEM_ALERT_WINDOW |
                    PERMISSION_ATOM_POST_NOTIFICATIONS |
                    PERMISSION_ATOM_EXACT_ALARM |
                    PERMISSION_ATOM_MEDIA_IMAGES |
                    PERMISSION_ATOM_MEDIA_VIDEO;

    private PermissionContract() {
    }

    /** -1 was the old «everything» sentinel; unknown bits are still ignored by consumers. */
    public static int normalizeRequiredPermissions(final int requiredPermissions) {
        return requiredPermissions == -1 ? PERMISSION_ALL : requiredPermissions;
    }

    public static boolean hasAtom(final int permissions, final int atom) {
        return (permissions & atom) == atom;
    }
}
