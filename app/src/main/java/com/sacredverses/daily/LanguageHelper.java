package com.sacredverses.daily;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Contextual verse-language options: only languages that actually have
 *  verses for the current faith context are offered, so a user never sees
 *  a language that would silently fall back to English.
 *
 *  Resulting sets (data-driven via VerseRepository.faithHasLang):
 *  All → en/hi/sa/ar/pa/pi · Hinduism → en/hi/sa · Islam → en/ar ·
 *  Sikhism → en/pa · Buddhism → en/pi · Christianity → en. */
public class LanguageHelper {

    /** All languages with bundled content, in selector order. */
    public static final String[] ORDERED = {"en", "hi", "sa", "ar", "pa", "pi"};

    /** Languages with at least one verse for the given faith.
     *  "All" always returns every language. */
    public static List<String> languagesForFaith(Context ctx, String faith) {
        List<String> out = new ArrayList<>();
        for (String lang : ORDERED) {
            if (VerseRepository.faithHasLang(ctx, lang, faith)) out.add(lang);
        }
        return out;
    }

    /** Union of the language sets of the faiths present in the user's
     *  saved verses (favorites). Returns all six when nothing is saved. */
    public static List<String> languagesForSaved(Context ctx) {
        SharedPreferences prefs =
                ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
        Set<String> favIds = prefs.getStringSet("favorites", new HashSet<String>());
        Set<String> faiths = new HashSet<>();
        if (favIds != null) {
            for (Verse v : VerseRepository.load(ctx)) {
                if (favIds.contains(v.id)) faiths.add(v.faith);
            }
        }
        if (faiths.isEmpty()) {
            List<String> all = new ArrayList<>();
            for (String lang : ORDERED) all.add(lang);
            return all;
        }
        Set<String> union = new LinkedHashSet<>();
        for (String f : faiths) union.addAll(languagesForFaith(ctx, f));
        List<String> out = new ArrayList<>();
        for (String lang : ORDERED) {
            if (union.contains(lang)) out.add(lang);
        }
        return out;
    }

    /** Language options for a browse filter ("All", a faith, or "Saved"). */
    public static List<String> languagesForFilter(Context ctx, String filter) {
        if ("Saved".equals(filter)) return languagesForSaved(ctx);
        return languagesForFaith(ctx, filter);
    }
}
