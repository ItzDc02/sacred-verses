package com.sacredverses.daily;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Festival mode: on festival days the home screen shows a greeting banner
 *  and wears the festival's colors instead of the daily rotation palette. */
public class FestivalHelper {

    public static class Festival {
        public final String name;
        public final String greeting;
        public final String explainer;
        public final int colorStart;
        public final int colorEnd;

        Festival(String name, String greeting, String explainer,
                 int colorStart, int colorEnd) {
            this.name = name;
            this.greeting = greeting;
            this.explainer = explainer;
            this.colorStart = colorStart;
            this.colorEnd = colorEnd;
        }
    }

    private static class Entry {
        Festival festival;
        String date; // yyyy-MM-dd
    }

    private static List<Entry> entries;

    private static List<Entry> load(Context ctx) {
        if (entries != null) return entries;
        entries = new ArrayList<>();
        try {
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(ctx.getAssets().open("festivals.json"), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                JSONArray colors = o.getJSONArray("colors");
                JSONArray dates = o.getJSONArray("dates");
                for (int d = 0; d < dates.length(); d++) {
                    Entry e = new Entry();
                    e.festival = new Festival(
                            o.getString("name"),
                            o.getString("greeting"),
                            o.getString("explainer"),
                            (int) Long.parseLong(colors.getString(0).substring(1), 16) | 0xFF000000,
                            (int) Long.parseLong(colors.getString(1).substring(1), 16) | 0xFF000000);
                    e.date = dates.getString(d);
                    entries.add(e);
                }
            }
        } catch (Exception ignored) {
        }
        return entries;
    }

    /** Today's festival, or null on ordinary days. */
    public static Festival today(Context ctx) {
        return onDate(ctx, LocalDate.now());
    }

    public static Festival onDate(Context ctx, LocalDate date) {
        String key = date.toString();
        for (Entry e : load(ctx)) {
            if (key.equals(e.date)) return e.festival;
        }
        return null;
    }
}
