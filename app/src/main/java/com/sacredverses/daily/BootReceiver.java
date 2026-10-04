package com.sacredverses.daily;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-schedules the daily verse after a phone reboot. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AlarmScheduler.scheduleNext(ctx);
        }
    }
}
