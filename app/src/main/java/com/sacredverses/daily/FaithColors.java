package com.sacredverses.daily;

/** Accent color per faith, used for card strips and pills.
 *  {@link #getDark} returns lightened variants for text drawn on dark
 *  surfaces (dark-mode cards), where the normal accents would be too dim. */
public class FaithColors {
    public static int get(String faith) {
        if (faith == null) return 0xFF4A3F8C;
        switch (faith) {
            case "Hinduism": return 0xFFE8912D;
            case "Christianity": return 0xFF4A7FC9;
            case "Islam": return 0xFF2E8B57;
            case "Buddhism": return 0xFFC9A227;
            case "Sikhism": return 0xFFD97B29;
            default: return 0xFF4A3F8C;
        }
    }

    public static int getDark(String faith) {
        if (faith == null) return 0xFFB7ABEE;
        switch (faith) {
            case "Hinduism": return 0xFFF5B45E;
            case "Christianity": return 0xFF8FB4E8;
            case "Islam": return 0xFF6FCF97;
            case "Buddhism": return 0xFFE3C65A;
            case "Sikhism": return 0xFFF2A65E;
            default: return 0xFFB7ABEE;
        }
    }

    /** Accent for text on a dark surface, honoring the user's dark mode. */
    public static int forText(boolean dark, String faith) {
        return dark ? getDark(faith) : get(faith);
    }
}
