package com.sacredverses.daily;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

/** Reads verses aloud with the phone's built-in text-to-speech engine —
 *  no downloads, no accounts, no cost. Lazy-initialized on first tap;
 *  tapping while speaking stops. Speaks in the user's verse language,
 *  falling back gracefully when the voice pack is missing. */
public class TtsSpeaker {

    private static TextToSpeech tts;
    private static boolean ready = false;
    private static String lang = "en";
    private static Runnable pending;

    /** Speak (or stop, if already speaking). Safe to call any time. */
    public static void toggle(Context ctx, Verse verse) {
        final Context appCtx = ctx.getApplicationContext();
        final String wantLang = VerseRepository.verseLang(ctx);
        if (tts != null && ready) {
            if (tts.isSpeaking()) {
                tts.stop();
                return;
            }
            if (!wantLang.equals(lang)) setLanguage(wantLang);
            speak(appCtx, verse);
            return;
        }
        // first use: init async, then speak from the callback
        lang = wantLang;
        pending = () -> speak(appCtx, verse);
        try {
            tts = new TextToSpeech(appCtx, status -> {
                if (status != TextToSpeech.SUCCESS) {
                    fail(appCtx);
                    return;
                }
                setLanguage(lang);
                ready = true;
                if (pending != null) {
                    Runnable r = pending;
                    pending = null;
                    r.run();
                }
            });
        } catch (Exception e) {
            fail(appCtx);
        }
    }

    /** Best-effort locale for a verse language, with graceful fallback
     *  when the voice pack isn't installed. */
    private static Locale localeFor(String l) {
        if ("hi".equals(l)) return new Locale("hi");
        if ("sa".equals(l)) return new Locale("sa");
        if ("ar".equals(l)) return new Locale("ar");
        if ("pa".equals(l)) return new Locale("pa");
        return Locale.ENGLISH; // "en" and "pi" (no Pali voice packs exist)
    }

    private static void setLanguage(String wantLang) {
        if (tts == null) return;
        lang = wantLang;
        try {
            Locale locale = localeFor(wantLang);
            if (tts.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) {
                // Sanskrit shares the Devanagari script with Hindi — a Hindi
                // voice reads it far better than the default voice would.
                if ("sa".equals(wantLang)) locale = new Locale("hi");
                if (tts.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) {
                    locale = Locale.getDefault();
                }
            }
            tts.setLanguage(locale);
        } catch (Exception ignored) {
        }
    }

    private static void speak(Context ctx, Verse verse) {
        if (tts == null || !ready || verse == null) return;
        try {
            // drop parenthesised glosses ("(अर्थ: …)", "(rendering: …)")
            // so the reading flows naturally
            String text = VerseRepository.displayText(ctx, verse)
                    .replaceAll("\\s*\\((?:\u0905\u0930\u094d\u0925|rendering):[^)]*\\)", "").trim();
            String utterance = text + ". — " + verse.ref;
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "verse");
            tts.speak(utterance, TextToSpeech.QUEUE_FLUSH, params, "verse");
        } catch (Exception ignored) {
        }
    }

    private static void fail(Context ctx) {
        ready = false;
        pending = null;
        try {
            Toast.makeText(ctx, "Could not start voice reading", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    /** Call from the activity's onDestroy. */
    public static void shutdown() {
        try {
            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }
        } catch (Exception ignored) {
        }
        tts = null;
        ready = false;
        pending = null;
    }
}
