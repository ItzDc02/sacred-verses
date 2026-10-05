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

/** Loads the bundled verses.json (all public-domain translations, attributed). */
public class VerseRepository {

    /** Faith order for the daily rotation: every day brings a different tradition. */
    private static final String[] FAITH_ORDER = {
            "Hinduism", "Christianity", "Islam", "Buddhism", "Judaism", "Sikhism"
    };
    private static List<Verse> verses;

    public static List<Verse> load(Context ctx) {
        if (verses == null) {
            verses = new ArrayList<>();
            try {
                BufferedReader r = new BufferedReader(
                        new InputStreamReader(ctx.getAssets().open("verses.json"), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();
                JSONArray arr = new JSONArray(sb.toString());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    verses.add(new Verse(
                            o.getString("id"),
                            o.getString("faith"),
                            o.getString("ref"),
                            o.getString("text"),
                            o.getString("source")));
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not load verses.json", e);
            }
        }
        return verses;
    }

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
