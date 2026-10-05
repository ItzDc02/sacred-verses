package com.sacredverses.daily;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Loads the bundled verses.json (all public-domain translations, attributed),
 *  plus the Hindi starter collection (verses_hi.json). */
public class VerseRepository {

    /** Faith order for the daily rotation: every day brings a different tradition. */
    private static final String[] FAITH_ORDER = {
            "Hinduism", "Christianity", "Islam", "Buddhism", "Sikhism"
    };
    private static List<Verse> verses;
    private static List<Verse> hindiVerses;

    public static List<Verse> load(Context ctx) {
        if (verses == null) {
            verses = loadFile(ctx, "verses.json");
        }
        return verses;
    }

    /** Hindi starter collection (Kabir, Tulsidas, Rahim, Sanskrit shlokas). */
    public static List<Verse> loadHindi(Context ctx) {
        if (hindiVerses == null) {
            hindiVerses = loadFile(ctx, "verses_hi.json");
        }
        return hindiVerses;
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
                out.add(new Verse(
                        o.getString("id"),
                        o.getString("faith"),
                        o.getString("ref"),
                        o.getString("text"),
                        o.getString("source")));
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load " + name, e);
        }
        return out;
    }

    // ---------- language-aware entry points ----------

    /** "hi" for Hindi mode, anything else for English. */
    public static boolean isHindi(Context ctx) {
        return "hi".equals(ctx.getSharedPreferences("settings",
                Context.MODE_PRIVATE).getString("verse_lang", "en"));
    }

    /** Verse of the day honoring both the language and faith preferences. */
    public static Verse verseToday(Context ctx) {
        return verseOnDate(ctx, LocalDate.now());
    }

    /** Verse for a date honoring both the language and faith preferences. */
    public static Verse verseOnDate(Context ctx, LocalDate date) {
        if (isHindi(ctx)) return verseForDateHi(date, loadHindi(ctx));
        String faith = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getString("pref_faith", "All");
        return verseForDate(ctx, faith, date);
    }

    /** Deterministic Hindi verse for a date: cycles through the starter
     *  collection by day of year. */
    public static Verse verseForDateHi(LocalDate date, List<Verse> hindi) {
        if (hindi == null || hindi.isEmpty()) return null;
        return hindi.get(Math.floorMod(date.getDayOfYear() - 1, hindi.size()));
    }

    /** The verse list Browse should show for the current language. */
    public static List<Verse> browseList(Context ctx) {
        return isHindi(ctx) ? loadHindi(ctx) : load(ctx);
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

    /** All verses for one faith, or everything for "All". */
    public static List<Verse> forFaith(Context ctx, String faith) {
        List<Verse> all = load(ctx);
        if (faith == null || "All".equals(faith)) return all;
        List<Verse> out = new ArrayList<>();
        for (Verse v : all) if (faith.equals(v.faith)) out.add(v);
        return out.isEmpty() ? all : out;
    }
}
