package com.sacredverses.daily;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** A single verse from the bundled verse bank. The verse text is a
 *  per-language map: "en" (English, present on almost every verse),
 *  "hi" (Hindi), "sa" (Sanskrit), "ar" (Arabic), "pa" (Punjabi/Gurmukhi),
 *  "pi" (Pali). Entries whose source exists only in one language carry
 *  just that language; {@link #textFor} falls back gracefully. */
public class Verse {
    public final String id;
    public final String faith;
    public final String ref;
    public final String source;
    /** English text when present, else the first available language —
     *  kept so legacy call sites never see a null. */
    public final String text;
    public final Map<String, String> texts;

    public Verse(String id, String faith, String ref, String text, String source) {
        this(id, faith, ref, singletonEn(text), source);
    }

    public Verse(String id, String faith, String ref,
                 Map<String, String> texts, String source) {
        this.id = id;
        this.faith = faith;
        this.ref = ref;
        this.source = source;
        this.texts = Collections.unmodifiableMap(new HashMap<>(texts));
        String en = texts.get("en");
        this.text = en != null ? en : firstValue(texts);
    }

    private static Map<String, String> singletonEn(String text) {
        Map<String, String> m = new HashMap<>();
        m.put("en", text);
        return m;
    }

    private static String firstValue(Map<String, String> m) {
        for (String v : m.values()) {
            if (v != null && !v.isEmpty()) return v;
        }
        return "";
    }

    /** Best text for a language: exact match, else English, else whatever
     *  the verse has. Never null. */
    public String textFor(String lang) {
        String t = lang != null ? texts.get(lang) : null;
        if (t != null && !t.isEmpty()) return t;
        t = texts.get("en");
        if (t != null && !t.isEmpty()) return t;
        return firstValue(texts);
    }

    /** True when the verse actually has text in this language
     *  (not just a fallback). */
    public boolean hasLang(String lang) {
        String t = texts.get(lang);
        return t != null && !t.isEmpty();
    }
}
