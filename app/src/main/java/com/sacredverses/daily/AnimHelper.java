package com.sacredverses.daily;

import android.view.MotionEvent;
import android.view.View;

/** Subtle tactile press feedback for buttons and chips: scales to 0.94x
 *  on press (~80ms) and springs back to 1.0 on release (~120ms).
 *  The listener returns false so normal click handling is unaffected. */
public class AnimHelper {

    private static final float PRESSED_SCALE = 0.94f;

    public static void pressScale(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().cancel();
                    view.animate()
                            .scaleX(PRESSED_SCALE).scaleY(PRESSED_SCALE)
                            .setDuration(80)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().cancel();
                    view.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(120)
                            .start();
                    break;
                default:
                    break;
            }
            return false; // let the click proceed normally
        });
    }
}
