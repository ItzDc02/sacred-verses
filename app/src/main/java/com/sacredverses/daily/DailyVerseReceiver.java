package com.sacredverses.daily;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Fires each morning: wallpaper + notification, then schedules tomorrow's. */
public class DailyVerseReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        SharedPreferences prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
        Verse verse = VerseRepository.verseToday(ctx);
        if (prefs.getBoolean("wallpaper_enabled", false)) {
            boolean ok = WallpaperHelper.setVerseWallpaper(ctx, verse);
            if (ok) {
                prefs.edit()
                        .putString("wallpaper_day", java.time.LocalDate.now().toString())
                        .apply();
            }
        }
        if (prefs.getBoolean("notif_enabled", true)) {
            NotificationHelper.showVerse(ctx, verse);
        }
        VerseWidgetProvider.updateWidgets(ctx);
        AlarmScheduler.scheduleNext(ctx);
    }
}
