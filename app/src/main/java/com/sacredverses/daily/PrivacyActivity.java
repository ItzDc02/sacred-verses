package com.sacredverses.daily;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Simple screen showing the bundled privacy policy. */
public class PrivacyActivity extends Activity {

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences p =
                newBase.getSharedPreferences("settings", Context.MODE_PRIVATE);
        Configuration cfg = new Configuration(newBase.getResources().getConfiguration());
        int night = p.getBoolean("dark_mode", false)
                ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO;
        cfg.uiMode = (cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
        super.attachBaseContext(newBase.createConfigurationContext(cfg));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences p = getSharedPreferences("settings", MODE_PRIVATE);
        if (p.getBoolean("dark_mode", false)) {
            setTheme(R.style.Theme_SacredVerses_Dark);
        }
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Privacy Policy");
        title.setTextSize(22);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setTextColor(getColor(R.color.ink));
        root.addView(title);

        TextView body = new TextView(this);
        body.setTextSize(14);
        body.setTextColor(getColor(R.color.ink));
        body.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        body.setLayoutParams(lp);
        body.setText(loadPolicy());
        root.addView(body);

        scroll.addView(root);
        setContentView(scroll);
    }

    private String loadPolicy() {
        try {
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(getAssets().open("privacy_policy.txt"), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            r.close();
            return sb.toString();
        } catch (Exception e) {
            return "Privacy policy could not be loaded.";
        }
    }
}
