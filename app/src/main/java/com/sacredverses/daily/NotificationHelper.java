package com.sacredverses.daily;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Shows the daily verse. On most phones this notification is visible on the lock screen. */
public class NotificationHelper {
    public static final String CHANNEL_ID = "daily_verse";
    private static final int NOTIF_ID = 1001;

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    ctx.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription(ctx.getString(R.string.channel_desc));
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            ctx.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    public static void showVerse(Context ctx, Verse verse) {
        ensureChannel(ctx);
        String text = VerseRepository.displayText(ctx, verse);
        Intent intent = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(verse.faith + " · " + verse.ref)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle()
                        .bigText("\u201C" + text + "\u201D\n— " + verse.ref))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
        ctx.getSystemService(NotificationManager.class).notify(NOTIF_ID, n);
    }
}
