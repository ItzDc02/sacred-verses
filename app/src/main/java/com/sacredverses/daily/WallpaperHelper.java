package com.sacredverses.daily;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

/**
 * Renders the verse of the day onto a bitmap and sets it as the lock-screen
 * wallpaper. Renders at the device's actual screen size (not the system's
 * extra-wide parallax wallpaper size), keeping the top clear for the
 * clock/date and the bottom clear for shortcuts.
 *
 * Kept deliberately close to the v2.1 renderer, which is proven to work on
 * real phones — only the bitmap size and element positions changed.
 */
public class WallpaperHelper {

    /** Human-readable reason for the last failure (null if the last call worked). */
    public static String lastError = null;

    /** @return true if the wallpaper was set, false if anything went wrong. */
    public static boolean setVerseWallpaper(Context ctx, Verse verse) {
        lastError = null;
        if (Build.VERSION.SDK_INT < 24) {
            lastError = "needs Android 7 or newer";
            return false;
        }
        try {
            android.util.DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            int w = dm.widthPixels;
            int h = dm.heightPixels;
            if (w <= 0) w = 1080;
            if (h <= 0) h = 2400;
            if (w > 1440) { // cap bitmap memory on very wide screens
                h = h * 1440 / w;
                w = 1440;
            }

            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            float cx = w / 2f;
            int accent = FaithColors.get(verse.faith);

            // background gradient
            Paint bg = new Paint();
            bg.setShader(new LinearGradient(0, 0, 0, h,
                    0xFF2A2356, 0xFF6B4E9E, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, bg);

            // brand line (below the clock/date zone)
            TextPaint brand = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            brand.setColor(0x99E8E2F5);
            brand.setTextSize(w * 0.030f);
            brand.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            brand.setTextAlign(Paint.Align.CENTER);
            c.drawText("S A C R E D   V E R S E S", cx, h * 0.30f, brand);

            // verse text (serif, centered)
            TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(0xFFFFFFFF);
            tp.setTextSize(w * 0.052f);
            tp.setTypeface(Typeface.create("serif", Typeface.NORMAL));
            String text = "\u201C" + verse.text + "\u201D";
            StaticLayout layout = StaticLayout.Builder
                    .obtain(text, 0, text.length(), tp, (int) (w * 0.86))
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(6f, 1.2f)
                    .build();
            float blockH = layout.getHeight();
            float blockTop = h * 0.52f - blockH / 2f;
            c.save();
            c.translate((w - layout.getWidth()) / 2f, blockTop);
            layout.draw(c);
            c.restore();

            float afterText = blockTop + blockH;

            // accent divider
            Paint div = new Paint();
            div.setColor(accent);
            float divW = w * 0.18f;
            float divTop = afterText + h * 0.025f;
            c.drawRect(cx - divW / 2, divTop, cx + divW / 2, divTop + 3, div);

            // reference
            TextPaint rp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            rp.setColor(accent);
            rp.setTextSize(w * 0.042f);
            rp.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            rp.setTextAlign(Paint.Align.CENTER);
            c.drawText(verse.faith + " \u00B7 " + verse.ref, cx, divTop + h * 0.055f, rp);

            // source
            TextPaint sp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            sp.setColor(0xBBE8E2F5);
            sp.setTextSize(w * 0.032f);
            sp.setTextAlign(Paint.Align.CENTER);
            c.drawText(verse.source, cx, divTop + h * 0.085f, sp);

            WallpaperManager wm = WallpaperManager.getInstance(ctx);
            wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK);
            bmp.recycle();
            return true;
        } catch (Exception e) {
            lastError = e.getClass().getSimpleName()
                    + (e.getMessage() != null ? ": " + e.getMessage() : "");
            return false;
        }
    }
}
