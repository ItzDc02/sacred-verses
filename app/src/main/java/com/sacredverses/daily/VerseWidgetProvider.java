package com.sacredverses.daily;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

/**
 * Home-screen widget showing the verse of the day. Updated whenever the app
 * is opened and whenever the daily verse fires — tapping it opens the app.
 */
public class VerseWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] appWidgetIds) {
        updateWidgets(ctx);
    }

    public static void updateWidgets(Context ctx) {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            ComponentName cn = new ComponentName(ctx, VerseWidgetProvider.class);
            int[] ids = mgr.getAppWidgetIds(cn);
            if (ids.length == 0) return;
            SharedPreferences p =
                    ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
            Verse verse = VerseRepository.verseToday(ctx);
            for (int id : ids) {
                RemoteViews rv = new RemoteViews(ctx.getPackageName(),
                        R.layout.verse_widget);
                rv.setTextViewText(R.id.widgetText,
                        "\u201C" + VerseRepository.displayText(ctx, verse) + "\u201D");
                rv.setTextViewText(R.id.widgetRef,
                        verse.faith + " \u00B7 " + verse.ref);
                Intent open = new Intent(ctx, MainActivity.class);
                PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE);
                rv.setOnClickPendingIntent(R.id.widgetRoot, pi);
                mgr.updateAppWidget(id, rv);
            }
        } catch (Exception ignored) {
        }
    }
}
