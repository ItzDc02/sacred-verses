package com.sacredverses.daily;

import java.time.LocalDate;

/** One tasteful gradient palette per weekday — the home screen gets a fresh
 *  look every morning. All palettes are deep enough for white header text. */
public class ThemePalettes {

    public static class Palette {
        public final int start;
        public final int end;
        public final int accent;

        Palette(int start, int end, int accent) {
            this.start = start;
            this.end = end;
            this.accent = accent;
        }
    }

    private static final Palette[] PALETTES = {
            new Palette(0xFF2A2356, 0xFF6B4E9E, 0xFFE8B44A), // indigo/violet (brand)
            new Palette(0xFF0F3D3E, 0xFF1F7A6D, 0xFFFFD98A), // deep teal
            new Palette(0xFF5C1F2E, 0xFFB04A5A, 0xFFFFC98A), // maroon/rose
            new Palette(0xFF1E3A24, 0xFF4E7A3A, 0xFFE8D44A), // forest
            new Palette(0xFF1B2A4A, 0xFF3E5C9A, 0xFFFFB44A), // midnight blue
            new Palette(0xFF3D1F4E, 0xFF8A4B9E, 0xFFFFD44A), // plum
            new Palette(0xFF2B2620, 0xFF8A6D3B, 0xFFFFC44A), // charcoal/amber
    };

    /** Today's palette: dayOfYear % 7. */
    public static Palette today() {
        return forDate(LocalDate.now());
    }

    public static Palette forDate(LocalDate date) {
        return PALETTES[Math.floorMod(date.getDayOfYear() - 1, PALETTES.length)];
    }
}
