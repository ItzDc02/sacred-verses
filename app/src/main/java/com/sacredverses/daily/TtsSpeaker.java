package com.sacredverses.daily;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

/** Reads verses aloud with the phone's built-in text-to-speech engine —
 *  no downloads, no accounts, no cost. Lazy-initialized on first tap;
 *  tapping while speaking stops. */
public class TtsSpeaker {

    private static TextToSpeech tts;
    private static boolean ready = false;
    private static boolean hindi = false;
    private static Runnable pending;

    /** Speak (or stop, if already speaking). Safe to call any time. */
    public static void toggle(Context ctx, Verse verse) {
        final Context appCtx = ctx.getApplicationContext();
        final boolean wantHindi = VerseRepository.isHindi(ctx);
        if (tts != null && ready) {
            if (tts.isSpeaking()) {
                tts.stop();
                return;
            }
            if (wantHindi != hindi) setLanguage(wantHindi);
            speak(verse);
            return;
        }
        // first use: init async, then speak from the callback
        hindi = wantHindi;
        pending = () -> speak(verse);
        try {
            tts = new TextToSpeech(appCtx, status -> {
                if (status != TextToSpeech.SUCCESS) {
                    fail(appCtx, null);
                    return;
                }
                setLanguage(hindi);
                ready = true;
                if (pending != null) {
                    Runnable r = pending;
                    pending = null;
                    r.run();
                }
            });
        } catch (Exception e) {
            fail(appCtx, e);
        }
    }

    private static void setLanguage(boolean wantHindi) {
        if (tts == null) return;
        hindi = wantHindi;
        try {
            Locale locale = wantHindi ? new Locale("hi") : Locale.ENGLISH;
            if (tts.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) {
                locale = Locale.getDefault();
            }
            tts.setLanguage(locale);
        } catch (Exception ignored) {
        }
    }

    private static void speak(Verse verse) {
        if (tts == null || !ready || verse == null) return;
        try {
            // drop the parenthesised Hindi gloss so the reading flows naturally
            String text = verse.text.replaceAll("\\s*\\(अर्थ:[^)]*\\)", "").trim();
            String utterance = text + ". — " + verse.ref;
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "verse");
            tts.speak(utterance, TextToSpeech.QUEUE_FLUSH, params, "verse");
        } catch (Exception ignored) {
        }
    }

    private static void fail(Context ctx, Exception e) {
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
