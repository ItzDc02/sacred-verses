package com.sacredverses.daily;

/** Accent color per faith, used for card strips and pills. */
public class FaithColors {
    public static int get(String faith) {
        if (faith == null) return 0xFF4A3F8C;
        switch (faith) {
            case "Hinduism": return 0xFFE8912D;
            case "Christianity": return 0xFF4A7FC9;
            case "Islam": return 0xFF2E8B57;
            case "Buddhism": return 0xFFC9A227;
            case "Judaism": return 0xFF2B4C9B;
            case "Sikhism": return 0xFFD97B29;
            default: return 0xFF4A3F8C;
        }
    }
}
