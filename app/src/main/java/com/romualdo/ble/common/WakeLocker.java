package com.romualdo.ble.common;

/**
 * Created by user on 8/10/2017.
 */

import android.content.Context;
import android.os.PowerManager;

import com.romualdo.ble.gattclient.MainActivity;

// Code from https://stackoverflow.com/questions/6864712/android-alarmmanager-not-waking-phone-up
public abstract class WakeLocker {
    private static PowerManager.WakeLock wakeLock;

    public static void acquire(Context ctx) {
        if (wakeLock != null) wakeLock.release();

        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, MainActivity.APP_TAG);
        wakeLock.acquire();
    }

    public static void release() {
        if (wakeLock != null) wakeLock.release(); wakeLock = null;
    }
}
