package com.sacredverses.daily;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

/**
 * Renders a verse onto a square (1080x1080) image designed for sharing on
 * WhatsApp / Telegram / Instagram — the classic "good morning verse" card.
 * Same visual language as the lockscreen wallpaper, reframed for sharing,
 * with a small app credit line at the bottom so forwards carry the brand.
 */
public class ShareImageHelper {

    /** @return the rendered bitmap, or null if anything went wrong.
     *  The verse is rendered in the requested language; StaticLayout's
     *  centered alignment handles RTL scripts (Arabic) correctly. */
    public static Bitmap render(Context ctx, Verse verse, String lang) {
        try {
            int w = 1080;
            int h = 1080;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            float cx = w / 2f;
            int accent = FaithColors.get(verse.faith);

            // background gradient (same palette as the wallpaper)
            Paint bg = new Paint();
            bg.setShader(new LinearGradient(0, 0, 0, h,
                    0xFF2A2356, 0xFF6B4E9E, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, bg);

            // soft top glow band for depth
            Paint glow = new Paint();
            glow.setShader(new LinearGradient(0, 0, 0, h * 0.45f,
                    0x22FFFFFF, 0x00000000, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h * 0.45f, glow);

            // brand line
            TextPaint brand = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            brand.setColor(0x99E8E2F5);
            brand.setTextSize(34f);
            brand.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            brand.setTextAlign(Paint.Align.CENTER);
            c.drawText("S A C R E D   V E R S E S", cx, 150f, brand);

            // verse text (serif, centered — StaticLayout handles RTL shaping)
            TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(0xFFFFFFFF);
            tp.setTextSize(56f);
            tp.setTypeface(Typeface.create("serif", Typeface.NORMAL));
            String text = "\u201C" + verse.textFor(lang) + "\u201D";
            StaticLayout layout = StaticLayout.Builder
                    .obtain(text, 0, text.length(), tp, (int) (w * 0.84))
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(8f, 1.25f)
                    .build();
            float blockH = layout.getHeight();
            float blockTop = h * 0.50f - blockH / 2f;
            c.save();
            c.translate((w - layout.getWidth()) / 2f, blockTop);
            layout.draw(c);
            c.restore();

            float afterText = blockTop + blockH;

            // accent divider
            Paint div = new Paint();
            div.setColor(accent);
            float divW = w * 0.16f;
            float divTop = afterText + 36f;
            c.drawRect(cx - divW / 2, divTop, cx + divW / 2, divTop + 4, div);

            // reference
            TextPaint rp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            rp.setColor(accent);
            rp.setTextSize(46f);
            rp.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            rp.setTextAlign(Paint.Align.CENTER);
            c.drawText(verse.faith + " \u00B7 " + verse.ref, cx, divTop + 78f, rp);

            // source
            TextPaint sp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            sp.setColor(0xBBE8E2F5);
            sp.setTextSize(34f);
            sp.setTextAlign(Paint.Align.CENTER);
            c.drawText(verse.source, cx, divTop + 128f, sp);

            // app credit (so forwards carry the brand)
            TextPaint cp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            cp.setColor(0x88E8E2F5);
            cp.setTextSize(30f);
            cp.setTextAlign(Paint.Align.CENTER);
            c.drawText("Shared via Sacred Verses", cx, h - 60f, cp);

            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
}
