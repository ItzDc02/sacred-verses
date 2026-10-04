package com.sacredverses.daily;

/** A single verse from the bundled verse bank. */
public class Verse {
    public final String id;
    public final String faith;
    public final String ref;
    public final String text;
    public final String source;

    public Verse(String id, String faith, String ref, String text, String source) {
        this.id = id;
        this.faith = faith;
        this.ref = ref;
        this.text = text;
        this.source = source;
    }
}
