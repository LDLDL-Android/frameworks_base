/*
 * Copyright (C) 2026
 * SPDX-License-Identifier: Apache-2.0
 *
 * OPlus programmable side-key intercept helper.
 */

package com.android.server.policy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.os.UserHandle;
import android.util.Log;

public final class OplusPlusKey {
    private static final String TAG = "OplusPlusKey";
    private static final String PACKAGE_NAME = "com.oplus.pluskey";

    public static final String ACTION_SHORT_PRESS = PACKAGE_NAME + ".SHORT_PRESS";
    public static final String ACTION_LONG_PRESS = PACKAGE_NAME + ".LONG_PRESS";
    public static final String ACTION_CAMERA_TRIGGER_DOWN =
            PACKAGE_NAME + ".CAMERA_TRIGGER_DOWN";
    public static final String ACTION_CAMERA_TRIGGER_UP =
            PACKAGE_NAME + ".CAMERA_TRIGGER_UP";

    // Covers press/long-press detection, broadcast delivery and the action itself.
    private static final long WAKE_LOCK_TIMEOUT_MS = 5000;

    private static PowerManager.WakeLock sWakeLock;

    private OplusPlusKey() {
    }

    /**
     * The key wakes the CPU but not the screen. Hold a partial wake lock so the
     * device does not suspend again before the gesture is resolved and handled.
     */
    public static synchronized void acquireWakeLock(Context context) {
        if (sWakeLock == null) {
            PowerManager pm = context.getSystemService(PowerManager.class);
            if (pm == null) return;
            sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            sWakeLock.setReferenceCounted(false);
        }
        sWakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
    }

    /**
     * Use package presence as a device-independent capability check. Requiring
     * a privileged system app prevents a sideloaded package from taking over
     * the Assist key on products that do not include PlusKey.
     */
    public static boolean isAvailable(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(
                    PACKAGE_NAME, PackageManager.MATCH_SYSTEM_ONLY);
            return info.enabled && info.isPrivilegedApp();
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void fireShortPress(Context context) {
        sendPlusKeyBroadcast(context, ACTION_SHORT_PRESS, "fireShortPress");
    }

    public static void fireLongPress(Context context) {
        sendPlusKeyBroadcast(context, ACTION_LONG_PRESS, "fireLongPress");
    }

    public static void fireCameraTriggerDown(Context context) {
        sendPlusKeyBroadcast(context, ACTION_CAMERA_TRIGGER_DOWN, "fireCameraTriggerDown");
    }

    public static void fireCameraTriggerUp(Context context) {
        sendPlusKeyBroadcast(context, ACTION_CAMERA_TRIGGER_UP, "fireCameraTriggerUp");
    }

    private static void sendPlusKeyBroadcast(Context context, String action, String logName) {
        acquireWakeLock(context);
        try {
            Intent intent = new Intent(action)
                    .setPackage(PACKAGE_NAME)
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                            | Intent.FLAG_RECEIVER_FOREGROUND);
            context.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            Log.d(TAG, logName);
        } catch (RuntimeException e) {
            Log.w(TAG, logName + " failed", e);
        }
    }
}
