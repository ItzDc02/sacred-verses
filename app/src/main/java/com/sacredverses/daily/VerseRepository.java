package com.sacredverses.daily;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads the bundled verses.json (all public-domain translations, attributed).
 *  Every verse carries its text as a per-language map — "en" plus, where the
 *  source language is known verbatim, "hi" (Hindi), "sa" (Sanskrit),
 *  "ar" (Arabic), "pa" (Punjabi/Gurmukhi) and "pi" (Pali). */
public class VerseRepository {

    /** Faith order for the daily rotation: every day brings a different tradition. */
    private static final String[] FAITH_ORDER = {
            "Hinduism", "Christianity", "Islam", "Buddhism", "Sikhism"
    };

    /** Languages with bundled content, in selector order. */
    public static final String[] LANG_CODES = {"en", "hi", "sa", "ar", "pa", "pi"};

    public static String langDisplayName(String code) {
        if ("hi".equals(code)) return "हिन्दी";
        if ("sa".equals(code)) return "संस्कृतम्";
        if ("ar".equals(code)) return "العربية";
        if ("pa".equals(code)) return "ਪੰਜਾਬੀ";
        if ("pi".equals(code)) return "Pali";
        return "English";
    }

    private static List<Verse> verses;
    private static List<Verse> english;

    public static List<Verse> load(Context ctx) {
        if (verses == null) {
            verses = loadFile(ctx, "verses.json");
        }
        return verses;
    }

    /** Verses that have English text (the default pool). */
    public static List<Verse> loadEnglish(Context ctx) {
        if (english == null) {
            english = pool(load(ctx), "en");
        }
        return english;
    }

    private static List<Verse> loadFile(Context ctx, String name) {
        List<Verse> out = new ArrayList<>();
        try {
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(ctx.getAssets().open(name), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Object t = o.get("text");
                Map<String, String> texts = new HashMap<>();
                if (t instanceof JSONObject) {
                    JSONObject tm = (JSONObject) t;
                    java.util.Iterator<String> keys = tm.keys();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        texts.put(k, tm.optString(k, ""));
                    }
                } else {
                    // legacy plain-string entries
                    texts.put("en", o.getString("text"));
                }
                out.add(new Verse(
                        o.getString("id"),
                        o.getString("faith"),
                        o.getString("ref"),
                        texts,
                        o.getString("source")));
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load " + name, e);
        }
        return out;
    }

    // ---------- language ----------

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    /** The user's verse language ("en" default); always a known code. */
    public static String verseLang(Context ctx) {
        String lang = prefs(ctx).getString("verse_lang", "en");
        for (String c : LANG_CODES) if (c.equals(lang)) return lang;
        return "en";
    }

    /** The text of a verse in the user's language (with fallback). */
    public static String displayText(Context ctx, Verse verse) {
        return verse == null ? "" : verse.textFor(verseLang(ctx));
    }

    /** Verses that actually have text in the given language. */
    public static List<Verse> pool(List<Verse> all, String lang) {
        List<Verse> out = new ArrayList<>();
        for (Verse v : all) if (v.hasLang(lang)) out.add(v);
        return out;
    }

    public static List<Verse> pool(Context ctx, String lang) {
        return pool(load(ctx), lang);
    }

    /** True when the faith has at least one verse in the language. */
    public static boolean faithHasLang(Context ctx, String lang, String faith) {
        if ("All".equals(faith)) return true;
        for (Verse v : pool(ctx, lang)) {
            if (faith.equals(v.faith)) return true;
        }
        return false;
    }

    // ---------- language-aware entry points ----------

    /** Verse of the day honoring language + faith preferences. */
    public static Verse verseToday(Context ctx) {
        return verseOnDate(ctx, LocalDate.now());
    }

    /** Verse for a date honoring language + faith preferences. English keeps
     *  the rotating-faiths deterministic schedule; other languages cycle
     *  through their own pool (filtered by faith when one is chosen). */
    public static Verse verseOnDate(Context ctx, LocalDate date) {
        String lang = verseLang(ctx);
        String faith = prefs(ctx).getString("pref_faith", "All");
        if ("en".equals(lang)) return verseForDate(ctx, faith, date);
        List<Verse> p = pool(ctx, lang);
        List<Verse> f = filterFaith(p, faith);
        if (f.isEmpty()) f = p;
        if (f.isEmpty()) return verseForDate(ctx, faith, date); // ultimate fallback
        long days = ChronoUnit.DAYS.between(LocalDate.of(2026, 1, 1), date);
        return f.get((int) Math.floorMod(days, f.size()));
    }

    private static List<Verse> filterFaith(List<Verse> in, String faith) {
        if (faith == null || "All".equals(faith)) return in;
        List<Verse> out = new ArrayList<>();
        for (Verse v : in) if (faith.equals(v.faith)) out.add(v);
        return out;
    }

    /** The verse list Browse should show for the current language. */
    public static List<Verse> browseList(Context ctx) {
        return pool(ctx, verseLang(ctx));
    }

    /** Faiths present in the current language's collection (for filter chips). */
    public static List<String> presentFaiths(Context ctx) {
        List<String> out = new ArrayList<>();
        for (Verse v : browseList(ctx)) {
            if (!out.contains(v.faith)) out.add(v.faith);
        }
        return out;
    }

    // ---------- English collection ----------

    /** Deterministic verse of the day: same verse for everyone on the same date. */
    public static Verse verseOfDay(Context ctx) {
        return verseOfDay(ctx, "All");
    }

    /** Verse of the day for one faith (or "All" for the global rotation). */
    public static Verse verseOfDay(Context ctx, String faith) {
        return verseForDate(ctx, faith, LocalDate.now());
    }

    /** The verse that was (or will be) the verse of the day on a given date.
     *  Powers the History screen — same deterministic rotation as verseOfDay. */
    public static Verse verseForDate(Context ctx, String faith, LocalDate date) {
        long days = ChronoUnit.DAYS.between(LocalDate.of(2026, 1, 1), date);
        if (faith == null || "All".equals(faith)) {
            String f = FAITH_ORDER[(int) Math.floorMod(days, FAITH_ORDER.length)];
            List<Verse> list = forFaith(ctx, f);
            long idx = Math.floorMod(days / FAITH_ORDER.length, list.size());
            return list.get((int) idx);
        }
        List<Verse> list = forFaith(ctx, faith);
        return list.get((int) Math.floorMod(days, list.size()));
    }

    /** All English verses for one faith, or everything for "All". */
    public static List<Verse> forFaith(Context ctx, String faith) {
        List<Verse> all = loadEnglish(ctx);
        if (faith == null || "All".equals(faith)) return all;
        List<Verse> out = new ArrayList<>();
        for (Verse v : all) if (faith.equals(v.faith)) out.add(v);
        return out.isEmpty() ? all : out;
    }
}
