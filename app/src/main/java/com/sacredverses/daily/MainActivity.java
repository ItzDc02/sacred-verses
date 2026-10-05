package com.sacredverses.daily;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String[] FAITHS = {
            "All", "Hinduism", "Christianity", "Islam",
            "Buddhism", "Sikhism", "Saved"
    };

    private SharedPreferences prefs;
    private Set<String> favs;
    private String currentFilter = "All";
    private String myFaith = "All";
    private boolean suppressWallpaperToggle = false;
    private VerseAdapter adapter;

    // screens
    private ScrollView homeScroll, historyScroll;
    private LinearLayout browseContainer, settingsScrollInner, historyList;
    private ScrollView settingsScroll;
    private Button navHome, navBrowse, navHistory, navSettings;

    private LinearLayout headerBar, festivalSlot, todayCard;
    private TextView dateLine, streakLine, todayLabel, todayFaith, todayText,
            todayRef, todaySource, emptyView;
    private View todayDivider;
    private Button todayFav, todayShare, todayImageShare, todayListen,
            privacyButton, aboutButton, supportButton, inviteButton;
    private Switch notifSwitch, wallpaperSwitch, darkSwitch;
    private LinearLayout filterRow, timePresetsRow, faithRow, textSizeRow,
            languageRow, sponsorSlot;
    private ListView verseList;

    private static final String[] PRESET_LABELS =
            {"Morning", "Midday", "Evening", "Night"};
    private static final int[][] PRESET_TIMES =
            {{7, 0}, {13, 0}, {19, 0}, {21, 0}};

    private static final String[] TEXT_SIZE_LABELS = {"Small", "Medium", "Large"};
    private static final float[] TEXT_SIZE_SCALES = {0.85f, 1.0f, 1.2f};

    /** Accessibility: apply the user's text-size preference to the whole
     *  activity, and force the UI mode (dark/light) from the preference so
     *  values-night resources resolve correctly. */
    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences p =
                newBase.getSharedPreferences("settings", Context.MODE_PRIVATE);
        float scale = p.getFloat("font_scale", 1.0f);
        Configuration cfg = new Configuration(newBase.getResources().getConfiguration());
        if (Math.abs(scale - 1.0f) > 0.01f) cfg.fontScale = scale;
        int night = p.getBoolean("dark_mode", false)
                ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO;
        cfg.uiMode = (cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
        super.attachBaseContext(newBase.createConfigurationContext(cfg));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        if (prefs.getBoolean("dark_mode", false)) {
            setTheme(R.style.Theme_SacredVerses_Dark);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        favs = new HashSet<>(prefs.getStringSet("favorites", new HashSet<String>()));
        myFaith = prefs.getString("pref_faith", "All");
        // a saved faith that no longer exists (removed in a past update)
        // falls back to All instead of breaking
        boolean knownFaith = false;
        for (String f : FAITHS) {
            if (f.equals(myFaith)) {
                knownFaith = true;
                break;
            }
        }
        if (!knownFaith) {
            myFaith = "All";
            prefs.edit().putString("pref_faith", "All").apply();
        }
        recordFirstOpenAndSeen();
        NotificationHelper.ensureChannel(this);
        AlarmScheduler.scheduleNext(this);

        headerBar = findViewById(R.id.headerBar);
        festivalSlot = findViewById(R.id.festivalSlot);
        homeScroll = findViewById(R.id.homeScroll);
        browseContainer = findViewById(R.id.browseContainer);
        historyScroll = findViewById(R.id.historyScroll);
        historyList = findViewById(R.id.historyList);
        settingsScroll = findViewById(R.id.settingsScroll);
        navHome = findViewById(R.id.navHome);
        navBrowse = findViewById(R.id.navBrowse);
        navHistory = findViewById(R.id.navHistory);
        navSettings = findViewById(R.id.navSettings);

        dateLine = findViewById(R.id.dateLine);
        streakLine = findViewById(R.id.streakLine);
        todayLabel = findViewById(R.id.todayLabel);
        todayCard = findViewById(R.id.todayCard);
        todayDivider = findViewById(R.id.todayDivider);
        todaySource = findViewById(R.id.todaySource);
        todayFaith = findViewById(R.id.todayFaith);
        todayText = findViewById(R.id.todayText);
        todayRef = findViewById(R.id.todayRef);
        todayFav = findViewById(R.id.todayFav);
        todayShare = findViewById(R.id.todayShare);
        todayImageShare = findViewById(R.id.todayImageShare);
        todayListen = findViewById(R.id.todayListen);
        sponsorSlot = findViewById(R.id.sponsorSlot);
        filterRow = findViewById(R.id.filterRow);
        faithRow = findViewById(R.id.faithRow);
        timePresetsRow = findViewById(R.id.timePresetsRow);
        textSizeRow = findViewById(R.id.textSizeRow);
        languageRow = findViewById(R.id.languageRow);
        verseList = findViewById(R.id.verseList);
        emptyView = findViewById(R.id.emptyView);
        notifSwitch = findViewById(R.id.notifSwitch);
        wallpaperSwitch = findViewById(R.id.wallpaperSwitch);
        darkSwitch = findViewById(R.id.darkSwitch);
        privacyButton = findViewById(R.id.privacyButton);
        aboutButton = findViewById(R.id.aboutButton);
        supportButton = findViewById(R.id.supportButton);
        inviteButton = findViewById(R.id.inviteButton);

        dateLine.setText(
                new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date()));

        applyTheme();
        setupNav();
        setupFestival();
        setupVerseOfDay();
        setupSponsorCard();
        setupFilters();
        setupFaithRow();
        adapter = new VerseAdapter();
        verseList.setAdapter(adapter);
        setupSettings();
        setupFooter();
        updateStreak();
        VerseWidgetProvider.updateWidgets(this);
        requestNotificationPermission();
        maybeShowFirstLaunch();
        showScreen(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // the date may have changed while we were away — refresh the hero card
        // and re-apply the wallpaper if it wasn't set today (self-heals after
        // manual date changes, alarm hiccups, or reinstalls)
        recordFirstOpenAndSeen();
        applyTheme();
        setupFestival();
        setupVerseOfDay();
        updateStreak();
        VerseWidgetProvider.updateWidgets(this);
        maybeRefreshWallpaper();
    }

    @Override
    protected void onDestroy() {
        TtsSpeaker.shutdown();
        super.onDestroy();
    }

    // ---------- first open + seen dates (history clock) ----------

    /** The history clock starts on the user's very first app open, and every
     *  day the app is opened is recorded — history only shows days the user
     *  actually experienced. */
    private void recordFirstOpenAndSeen() {
        String today = LocalDate.now().toString();
        SharedPreferences.Editor ed = prefs.edit();
        if (!prefs.contains("first_open_date")) {
            ed.putString("first_open_date", today);
        }
        Set<String> seen =
                new HashSet<>(prefs.getStringSet("seen_dates", new HashSet<String>()));
        if (seen.add(today)) ed.putStringSet("seen_dates", seen);
        ed.apply();
    }

    // ---------- daily theme + festival ----------

    /** Paints the header with today's palette (or the festival's colors on
     *  festival days). The Verse-of-the-Day card always wears the brand
     *  gradient — the same visual language as the share image. */
    private void applyTheme() {
        int start, end;
        FestivalHelper.Festival f = FestivalHelper.today(this);
        if (f != null) {
            start = f.colorStart;
            end = f.colorEnd;
        } else {
            ThemePalettes.Palette p = ThemePalettes.today();
            start = p.start;
            end = p.end;
        }
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        headerBar.setBackground(gd);

        GradientDrawable card = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF2A2356, 0xFF6B4E9E});
        card.setCornerRadius(dp(16));
        todayCard.setBackground(card);
    }

    /** Festival banner card on the home screen (greeting + explainer). */
    private void setupFestival() {
        festivalSlot.removeAllViews();
        FestivalHelper.Festival f = FestivalHelper.today(this);
        if (f == null) return;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{f.colorStart, f.colorEnd});
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(16), dp(16), 0);
        card.setLayoutParams(lp);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));

        TextView greeting = new TextView(this);
        greeting.setText(f.greeting);
        greeting.setTextSize(17);
        greeting.setTypeface(greeting.getTypeface(),
                android.graphics.Typeface.BOLD);
        greeting.setTextColor(0xFFFFFFFF);
        card.addView(greeting);

        TextView explainer = new TextView(this);
        explainer.setText(f.explainer);
        explainer.setTextSize(13);
        explainer.setTextColor(0xE8FFFFFF);
        explainer.setLineSpacing(0, 1.25f);
        explainer.setPadding(0, dp(6), 0, 0);
        card.addView(explainer);

        festivalSlot.addView(card);
    }

    // ---------- night-aware chip colors ----------

    private boolean isDark() {
        return prefs.getBoolean("dark_mode", false);
    }

    private int chipSelBg() {
        return isDark() ? 0xFF6E5FC4 : 0xFF4A3F8C;
    }

    private int chipBg() {
        return isDark() ? 0xFF2E2752 : 0xFFE7E3F7;
    }

    private void paintChip(Button b, boolean sel) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(sel ? chipSelBg() : chipBg());
        bg.setCornerRadius(dp(14));
        b.setBackground(bg);
        b.setTextColor(getColor(sel ? android.R.color.white : R.color.primary));
        int h = dp(10), w = dp(14);
        b.setPadding(w, h, w, h);
    }

    /** Daily reflection streak: consecutive days the app was opened. */
    private void updateStreak() {
        String today = LocalDate.now().toString();
        String last = prefs.getString("streak_last", "");
        int streak = prefs.getInt("streak_count", 0);
        if (!today.equals(last)) {
            if (last.equals(LocalDate.now().minusDays(1).toString())) streak++;
            else streak = 1;
            prefs.edit().putInt("streak_count", streak)
                    .putString("streak_last", today).apply();
        }
        streakLine.setText(streak <= 1
                ? "\uD83D\uDD25 Day 1 — come back tomorrow to build your streak"
                : "\uD83D\uDD25 " + streak + "-day streak — keep it going!");
    }

    private void maybeRefreshWallpaper() {
        if (!prefs.getBoolean("wallpaper_enabled", false)) return;
        String today = LocalDate.now().toString();
        if (today.equals(prefs.getString("wallpaper_day", ""))) return;
        boolean ok = WallpaperHelper.setVerseWallpaper(this,
                VerseRepository.verseToday(this));
        if (ok) prefs.edit().putString("wallpaper_day", today).apply();
    }

    // ---------- bottom navigation ----------

    private void setupNav() {
        navHome.setOnClickListener(v -> showScreen(0));
        navBrowse.setOnClickListener(v -> showScreen(1));
        navHistory.setOnClickListener(v -> showScreen(2));
        navSettings.setOnClickListener(v -> showScreen(3));
    }

    private void showScreen(int which) {
        homeScroll.setVisibility(which == 0 ? View.VISIBLE : View.GONE);
        browseContainer.setVisibility(which == 1 ? View.VISIBLE : View.GONE);
        historyScroll.setVisibility(which == 2 ? View.VISIBLE : View.GONE);
        settingsScroll.setVisibility(which == 3 ? View.VISIBLE : View.GONE);
        paintNav(navHome, which == 0);
        paintNav(navBrowse, which == 1);
        paintNav(navHistory, which == 2);
        paintNav(navSettings, which == 3);
        if (which == 2) setupHistory();
    }

    private void paintNav(Button b, boolean sel) {
        b.setTextColor(getColor(sel ? R.color.primary : R.color.muted));
        b.setTypeface(b.getTypeface(), sel
                ? android.graphics.Typeface.BOLD
                : android.graphics.Typeface.NORMAL);
    }

    // ---------- verse of the day card ----------

    private void setupVerseOfDay() {
        final Verse verse = VerseRepository.verseToday(this);
        int accent = FaithColors.get(verse.faith);
        todayFaith.setText(verse.faith);
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(accent);
        pill.setCornerRadius(dp(14));
        todayFaith.setBackground(pill);
        todayText.setText("\u201C" + VerseRepository.displayText(this, verse) + "\u201D");
        todayDivider.setBackgroundColor(accent);
        todayRef.setText(verse.faith + " · " + verse.ref);
        todayRef.setTextColor(accent);
        todaySource.setText(verse.source);
        refreshFavButton(verse.id);
        todayFav.setOnClickListener(v -> {
            toggleFav(verse.id);
            refreshFavButton(verse.id);
            adapter.refresh();
        });
        todayShare.setOnClickListener(v -> shareVerse(verse));
        // image sharing needs scoped storage (Android 10+) — no permission dance
        todayImageShare.setVisibility(
                Build.VERSION.SDK_INT >= 29 ? View.VISIBLE : View.GONE);
        todayImageShare.setOnClickListener(v -> shareVerseAsImage(verse));
        todayListen.setOnClickListener(v -> TtsSpeaker.toggle(this, verse));
    }

    private void refreshFavButton(String id) {
        todayFav.setText(favs.contains(id) ? "★ Saved" : "☆ Save");
    }

    private void toggleFav(String id) {
        if (favs.contains(id)) favs.remove(id);
        else favs.add(id);
        prefs.edit().putStringSet("favorites", favs).apply();
    }

    private void shareVerse(Verse verse) {
        String text = "\u201C" + VerseRepository.displayText(this, verse) + "\u201D\n— " + verse.ref
                + " (" + verse.faith + ")\n\nShared via Sacred Verses app";
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, "Share this verse"));
    }

    /** Renders the verse as a square "good morning" image and shares it via
     *  the system sheet (WhatsApp, Telegram, Instagram…). Saved to the shared
     *  media store so it needs no storage permission on Android 10+. */
    private void shareVerseAsImage(Verse verse) {
        if (Build.VERSION.SDK_INT < 29) {
            Toast.makeText(this, "Image sharing needs Android 10 or newer",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap bmp = ShareImageHelper.render(this, verse,
                VerseRepository.verseLang(this));
        if (bmp == null) {
            Toast.makeText(this, "Could not create image", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Images.Media.DISPLAY_NAME,
                    "sacred-verse-" + System.currentTimeMillis() + ".png");
            cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            cv.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SacredVerses");
            ContentResolver cr = getContentResolver();
            Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
            if (uri == null) throw new Exception("media insert failed");
            try (java.io.OutputStream os = cr.openOutputStream(uri)) {
                if (os == null) throw new Exception("open failed");
                bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("image/png");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Share verse image"));
        } catch (Exception e) {
            Toast.makeText(this, "Could not share image", Toast.LENGTH_SHORT).show();
        } finally {
            bmp.recycle();
        }
    }

    // ---------- sponsored card (home screen) ----------

    private void setupSponsorCard() {
        // a dismiss only hides the card until tomorrow — it returns with the next verse
        String today = LocalDate.now().toString();
        if (today.equals(prefs.getString("sponsor_dismissed_day", ""))) return;
        try {
            JSONArray arr = new JSONArray(loadAsset("sponsors.json"));
            if (arr.length() == 0) return;
            // rotate sponsors daily so every entry gets seen over time
            JSONObject s = arr.getJSONObject(
                    (int) (Math.abs(LocalDate.now().toEpochDay()) % arr.length()));
            View card = getLayoutInflater().inflate(R.layout.sponsor_card, sponsorSlot, false);
            ((TextView) card.findViewById(R.id.sponsorTitle)).setText(s.getString("title"));
            ((TextView) card.findViewById(R.id.sponsorText)).setText(s.getString("text"));
            Button btn = card.findViewById(R.id.sponsorButton);
            btn.setText(s.optString("button", "View"));
            final String link = s.getString("link");
            btn.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
                }
            });
            card.findViewById(R.id.sponsorClose).setOnClickListener(v -> {
                prefs.edit().putString("sponsor_dismissed_day",
                        LocalDate.now().toString()).apply();
                sponsorSlot.removeAllViews();
                Toast.makeText(this, "Sponsored card hidden until tomorrow",
                        Toast.LENGTH_SHORT).show();
            });
            sponsorSlot.addView(card);
        } catch (Exception ignored) {
        }
    }

    private String loadAsset(String name) throws Exception {
        BufferedReader r = new BufferedReader(
                new InputStreamReader(getAssets().open(name), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }

    // ---------- faith filter buttons (browse) ----------

    /** Filter options for the current language: All + faiths present in that
     *  collection + Saved. (Hindi mode only has Hinduism.) */
    private List<String> browseFaithOptions() {
        List<String> opts = new ArrayList<>();
        opts.add("All");
        opts.addAll(VerseRepository.presentFaiths(this));
        opts.add("Saved");
        return opts;
    }

    private void setupFilters() {
        filterRow.removeAllViews();
        for (String faith : browseFaithOptions()) {
            Button b = makeChip(faith);
            final String f = faith;
            b.setOnClickListener(v -> {
                currentFilter = f;
                paintFilters();
                adapter.refresh();
            });
            b.setTag(faith);
            filterRow.addView(b);
        }
        paintFilters();
    }

    private void paintFilters() {
        for (int i = 0; i < filterRow.getChildCount(); i++) {
            Button b = (Button) filterRow.getChildAt(i);
            String faith = (String) b.getTag();
            paintChip(b, currentFilter.equals(faith));
        }
    }

    // ---------- "my faith" preference ----------

    private void setupFaithRow() {
        faithRow.removeAllViews();
        for (String faith : FAITHS) {
            if ("Saved".equals(faith)) continue;
            Button b = makeChip(faith);
            final String f = faith;
            b.setOnClickListener(v -> {
                myFaith = f;
                prefs.edit().putString("pref_faith", f).apply();
                paintFaithRow();
                setupVerseOfDay();
                AlarmScheduler.scheduleNext(this);
                Toast.makeText(this,
                        "All".equals(f) ? "Daily verse: all faiths"
                                : "Daily verse: " + f + " only",
                        Toast.LENGTH_SHORT).show();
            });
            b.setTag(faith);
            faithRow.addView(b);
        }
        paintFaithRow();
    }

    private void paintFaithRow() {
        for (int i = 0; i < faithRow.getChildCount(); i++) {
            Button b = (Button) faithRow.getChildAt(i);
            String faith = (String) b.getTag();
            paintChip(b, myFaith.equals(faith));
        }
    }

    private Button makeChip(String text) {
        Button b = new Button(this, null, android.R.attr.borderlessButtonStyle);
        b.setText(text);
        b.setAllCaps(false);
        b.setMinHeight(dp(44)); // comfortable touch target
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(8), 0);
        b.setLayoutParams(lp);
        return b;
    }

    // ---------- verse list ----------

    private class VerseAdapter extends BaseAdapter {
        private final List<Verse> items = new ArrayList<>();
        private final LayoutInflater inflater = LayoutInflater.from(MainActivity.this);

        VerseAdapter() {
            refresh();
        }

        void refresh() {
            items.clear();
            List<Verse> base = VerseRepository.browseList(MainActivity.this);
            if (!browseFaithOptions().contains(currentFilter)) currentFilter = "All";
            if ("All".equals(currentFilter)) {
                items.addAll(base);
            } else if ("Saved".equals(currentFilter)) {
                for (Verse v : base) if (favs.contains(v.id)) items.add(v);
            } else {
                for (Verse v : base) if (currentFilter.equals(v.faith)) items.add(v);
            }
            emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Verse getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder h;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_verse, parent, false);
                h = new ViewHolder();
                h.strip = convertView.findViewById(R.id.accentStrip);
                h.text = convertView.findViewById(R.id.verseText);
                h.ref = convertView.findViewById(R.id.verseRef);
                h.source = convertView.findViewById(R.id.verseSource);
                h.fav = convertView.findViewById(R.id.favButton);
                h.share = convertView.findViewById(R.id.shareButton);
                h.image = convertView.findViewById(R.id.imageButton);
                convertView.setTag(h);
            } else {
                h = (ViewHolder) convertView.getTag();
            }
            final Verse verse = getItem(position);
            GradientDrawable strip = new GradientDrawable();
            strip.setColor(FaithColors.get(verse.faith));
            strip.setCornerRadii(new float[]{dp(16), dp(16), dp(16), dp(16), 0, 0, 0, 0});
            h.strip.setBackground(strip);
            h.text.setText("\u201C" + VerseRepository.displayText(MainActivity.this, verse) + "\u201D");
            h.ref.setText(verse.faith + " · " + verse.ref);
            h.source.setText(verse.source);
            h.fav.setText(favs.contains(verse.id) ? "★" : "☆");
            h.fav.setOnClickListener(v -> {
                toggleFav(verse.id);
                notifyDataSetChanged();
                refreshFavButton(VerseRepository.verseToday(MainActivity.this).id);
            });
            h.share.setOnClickListener(v -> shareVerse(verse));
            h.image.setVisibility(
                    Build.VERSION.SDK_INT >= 29 ? View.VISIBLE : View.GONE);
            h.image.setOnClickListener(v -> shareVerseAsImage(verse));
            return convertView;
        }

        class ViewHolder {
            View strip;
            TextView text, ref, source;
            Button fav, share, image;
        }
    }

    // ---------- verse history (days actually experienced) ----------

    /** Rebuilds the history list: only dates from the user's first open
     *  through today that are in seen_dates — each row shows that day's verse
     *  via the same deterministic rotation as the daily verse. */
    private void setupHistory() {
        historyList.removeAllViews();
        String first = prefs.getString("first_open_date", LocalDate.now().toString());
        Set<String> seen = prefs.getStringSet("seen_dates", new HashSet<String>());
        LocalDate today = LocalDate.now();
        LocalDate firstDate;
        try {
            firstDate = LocalDate.parse(first);
        } catch (Exception e) {
            firstDate = today;
        }
        int shown = 0;
        for (LocalDate date = today; !date.isBefore(firstDate); date = date.minusDays(1)) {
            if (!seen.contains(date.toString())) continue;
            shown++;
            final Verse verse = VerseRepository.verseOnDate(this, date);
            final String label = date.equals(today) ? "Today"
                    : date.equals(today.minusDays(1)) ? "Yesterday"
                    : date.getDayOfWeek().getDisplayName(
                            java.time.format.TextStyle.FULL, Locale.getDefault())
                    + ", " + date.getDayOfMonth() + " "
                    + date.getMonth().getDisplayName(
                            java.time.format.TextStyle.SHORT, Locale.getDefault());

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundResource(R.drawable.card_bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(16), dp(6), dp(16), dp(6));
            row.setLayoutParams(lp);
            row.setPadding(dp(16), dp(12), dp(16), dp(12));

            TextView dateView = new TextView(this);
            dateView.setText(label.toUpperCase(Locale.getDefault()));
            dateView.setTextSize(11);
            dateView.setTypeface(dateView.getTypeface(),
                    android.graphics.Typeface.BOLD);
            dateView.setTextColor(getColor(R.color.muted));
            row.addView(dateView);

            TextView snippet = new TextView(this);
            String t = VerseRepository.displayText(this, verse);
            snippet.setText("\u201C" + (t.length() > 110
                    ? t.substring(0, 110) + "…" : t) + "\u201D");
            snippet.setTextSize(15);
            snippet.setTextColor(getColor(R.color.ink));
            snippet.setPadding(0, dp(6), 0, 0);
            row.addView(snippet);

            TextView refView = new TextView(this);
            refView.setText(verse.faith + " · " + verse.ref);
            refView.setTextSize(12);
            refView.setTypeface(refView.getTypeface(),
                    android.graphics.Typeface.BOLD);
            refView.setTextColor(FaithColors.forText(isDark(), verse.faith));
            refView.setPadding(0, dp(4), 0, 0);
            row.addView(refView);

            row.setOnClickListener(v -> showHistoryVerse(verse, label));
            historyList.addView(row);
        }
        if (shown == 0) {
            TextView empty = new TextView(this);
            empty.setText("No history yet — come back tomorrow.");
            empty.setTextColor(getColor(R.color.muted));
            empty.setPadding(dp(16), dp(16), dp(16), dp(16));
            historyList.addView(empty);
        }
    }

    /** Full verse from history, with save / share / image / listen actions. */
    private void showHistoryVerse(final Verse verse, String dateLabel) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        layout.setPadding(pad, pad, pad, 0);

        TextView faithView = new TextView(this);
        faithView.setText(verse.faith);
        faithView.setTextSize(12);
        faithView.setTypeface(faithView.getTypeface(),
                android.graphics.Typeface.BOLD);
        faithView.setTextColor(getColor(android.R.color.white));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(FaithColors.get(verse.faith));
        pill.setCornerRadius(dp(14));
        faithView.setBackground(pill);
        faithView.setPadding(dp(12), dp(6), dp(12), dp(6));
        layout.addView(faithView);

        TextView textView = new TextView(this);
        textView.setText("\u201C" + VerseRepository.displayText(this, verse) + "\u201D");
        textView.setTextSize(18);
        textView.setTextColor(getColor(R.color.ink));
        textView.setPadding(0, dp(12), 0, 0);
        layout.addView(textView);

        TextView refView = new TextView(this);
        refView.setText("— " + verse.ref + " · " + verse.source);
        refView.setTextSize(13);
        refView.setTextColor(getColor(R.color.muted));
        refView.setPadding(0, dp(12), 0, 0);
        layout.addView(refView);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(8), 0, 0);
        Button shareText = new Button(this, null,
                android.R.attr.borderlessButtonStyle);
        shareText.setText("Share text");
        shareText.setAllCaps(false);
        shareText.setTextColor(getColor(R.color.primary));
        shareText.setOnClickListener(v -> shareVerse(verse));
        btnRow.addView(shareText);
        if (Build.VERSION.SDK_INT >= 29) {
            Button shareImg = new Button(this, null,
                    android.R.attr.borderlessButtonStyle);
            shareImg.setText("🖼 Share image");
            shareImg.setAllCaps(false);
            shareImg.setTextColor(getColor(R.color.primary));
            shareImg.setOnClickListener(v -> shareVerseAsImage(verse));
            btnRow.addView(shareImg);
        }
        Button listen = new Button(this, null,
                android.R.attr.borderlessButtonStyle);
        listen.setText("🔊 Listen");
        listen.setAllCaps(false);
        listen.setTextColor(getColor(R.color.primary));
        listen.setOnClickListener(v -> TtsSpeaker.toggle(MainActivity.this, verse));
        btnRow.addView(listen);
        layout.addView(btnRow);

        new AlertDialog.Builder(this)
                .setTitle(dateLabel)
                .setView(layout)
                .setPositiveButton(favs.contains(verse.id) ? "★ Saved" : "☆ Save",
                        (d, w) -> {
                            toggleFav(verse.id);
                            adapter.refresh();
                        })
                .setNegativeButton("Close", null)
                .show();
    }

    // ---------- settings ----------

    private void setupSettings() {
        notifSwitch.setChecked(prefs.getBoolean("notif_enabled", true));
        notifSwitch.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean("notif_enabled", checked).apply();
            AlarmScheduler.scheduleNext(this);
            Toast.makeText(this, checked ? "Daily verse ON" : "Daily verse OFF",
                    Toast.LENGTH_SHORT).show();
        });

        wallpaperSwitch.setChecked(prefs.getBoolean("wallpaper_enabled", false));
        wallpaperSwitch.setOnCheckedChangeListener((v, isChecked) -> {
            if (suppressWallpaperToggle) return;
            // revert the visual flip — the real change happens only
            // after the user confirms the disclaimer dialog
            suppressWallpaperToggle = true;
            wallpaperSwitch.setChecked(!isChecked);
            suppressWallpaperToggle = false;
            showWallpaperDialog(isChecked);
        });

        darkSwitch.setChecked(prefs.getBoolean("dark_mode", false));
        darkSwitch.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean("dark_mode", checked).apply();
            recreate();
        });

        setupTimePresets();
        setupSupportButton();
        setupTextSizeRow();
        setupLanguageRow();

        inviteButton.setOnClickListener(v -> {
            String text = "I start my mornings with Sacred Verses — "
                    + "one verse from the world's scriptures, every day.\n\n"
                    + "Free, no ads, no account needed:\n"
                    + "https://github.com/ItzDc02/sacred-verses/releases";
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(send, "Invite friends"));
        });
    }

    /** Text-size accessibility: Small / Medium / Large, applied app-wide via
     *  fontScale. Changing it recreates the activity so everything re-renders. */
    private void setupTextSizeRow() {
        textSizeRow.removeAllViews();
        float cur = prefs.getFloat("font_scale", 1.0f);
        for (int i = 0; i < TEXT_SIZE_LABELS.length; i++) {
            final float scale = TEXT_SIZE_SCALES[i];
            Button b = makeChip(TEXT_SIZE_LABELS[i]);
            boolean sel = Math.abs(cur - scale) < 0.01f;
            paintChip(b, sel);
            b.setOnClickListener(v -> {
                prefs.edit().putFloat("font_scale", scale).apply();
                recreate();
            });
            textSizeRow.addView(b);
        }
    }

    /** Verse language: one chip per language that has bundled content.
     *  Switching resets the faith preference when the chosen faith has no
     *  verses in the new language, then recreates so everything re-renders. */
    private void setupLanguageRow() {
        languageRow.removeAllViews();
        String cur = VerseRepository.verseLang(this);
        for (String code : VerseRepository.LANG_CODES) {
            final String val = code;
            Button b = makeChip(VerseRepository.langDisplayName(code));
            paintChip(b, cur.equals(val));
            b.setOnClickListener(v -> {
                prefs.edit().putString("verse_lang", val).apply();
                currentFilter = "All";
                if (!VerseRepository.faithHasLang(MainActivity.this, val, myFaith)) {
                    myFaith = "All";
                    prefs.edit().putString("pref_faith", "All").apply();
                    Toast.makeText(MainActivity.this,
                            "Faith filter reset — no "
                                    + VerseRepository.langDisplayName(val)
                                    + " verses for that faith yet",
                            Toast.LENGTH_SHORT).show();
                }
                recreate();
            });
            languageRow.addView(b);
        }
    }

    /** "Support" button: opens the user's UPI app for a voluntary contribution.
     *  Shown only when real UPI id(s) are configured in support.json.
     *  Rotates through the configured ids round-robin on every tap, so a
     *  problematic id is automatically skipped on the next attempt.
     *  Note: the payment itself completes inside the UPI app, which never
     *  reports success/failure back — true failure detection isn't possible. */
    private void setupSupportButton() {
        try {
            JSONObject cfg = new JSONObject(loadAsset("support.json"));
            final String upiName = cfg.optString("upi_name", "Sacred Verses");
            final java.util.ArrayList<String> ids = new java.util.ArrayList<>();
            JSONArray arr = cfg.optJSONArray("upi_ids");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    String id = arr.optString(i, "").trim();
                    if (!id.isEmpty() && !id.contains("REPLACE_WITH")) ids.add(id);
                }
            } else {
                // backward compatibility with the old single-id format
                String id = cfg.optString("upi_id", "").trim();
                if (!id.isEmpty() && !id.contains("REPLACE_WITH")) ids.add(id);
            }
            if (ids.isEmpty()) return;
            supportButton.setVisibility(View.VISIBLE);
            supportButton.setOnClickListener(v -> {
                int rot = prefs.getInt("upi_rot", 0);
                final String upiId = ids.get(Math.abs(rot) % ids.size());
                prefs.edit().putInt("upi_rot", rot + 1).apply();
                showSupportDialog(upiId);
            });
        } catch (Exception ignored) {
        }
    }

    /** Support dialog: shows the UPI ID as copyable text with written
     *  instructions (works on every UPI app — no intent-compatibility lottery),
     *  plus a one-tap button that fires the UPI intent directly. */
    private void showSupportDialog(final String upiId) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        TextView intro = new TextView(this);
        intro.setText("If this app brightens your mornings, consider supporting it — "
                + "every rupee keeps it free forever.");
        layout.addView(intro);

        TextView idLabel = new TextView(this);
        idLabel.setText("\nOur UPI ID (tap to copy):");
        layout.addView(idLabel);

        TextView idView = new TextView(this);
        idView.setText(upiId);
        idView.setTypeface(android.graphics.Typeface.MONOSPACE);
        idView.setTextSize(17);
        idView.setPadding(pad, pad / 2, pad, pad / 2);
        idView.setBackgroundColor(isDark() ? 0xFF2A2438 : 0xFFF0F0F0);
        idView.setOnClickListener(v -> {
            ClipboardManager cm =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("UPI ID", upiId));
            Toast.makeText(this, "UPI ID copied", Toast.LENGTH_SHORT).show();
        });
        layout.addView(idView);

        TextView howto = new TextView(this);
        howto.setText("\nHow to pay:\n1. Tap the ID above to copy it\n"
                + "2. Open GPay / PhonePe / Paytm / BHIM\n"
                + "3. Choose \"Pay to UPI ID\", paste it, send any amount");
        layout.addView(howto);

        new AlertDialog.Builder(this)
                .setTitle("Support Sacred Verses")
                .setView(layout)
                .setNegativeButton("Close", null)
                .show();
    }

    /** Disclaimer shown EVERY time the wallpaper switch is flipped, before anything changes. */
    private void showWallpaperDialog(boolean wantOn) {
        if (wantOn) {
            new AlertDialog.Builder(this)
                    .setTitle("Verse as lockscreen wallpaper")
                    .setMessage("Every morning, when your verse reminder arrives, your lockscreen "
                            + "wallpaper will be replaced with the verse of the day.\n\n"
                            + "Your current wallpaper will be replaced.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Turn on", (d, w) -> setWallpaperEnabled(true))
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Turn off verse wallpaper?")
                    .setMessage("Your lockscreen will keep its current image until you change it "
                            + "yourself in your phone's settings.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Turn off", (d, w) -> setWallpaperEnabled(false))
                    .show();
        }
    }

    private void setWallpaperEnabled(boolean enabled) {
        SharedPreferences.Editor ed = prefs.edit().putBoolean("wallpaper_enabled", enabled);
        if (enabled) {
            // mark today as handled WITHOUT setting the wallpaper: it will
            // change when the daily verse arrives, never instantly on toggle
            ed.putString("wallpaper_day", LocalDate.now().toString());
        }
        ed.apply();
        suppressWallpaperToggle = true;
        wallpaperSwitch.setChecked(enabled);
        suppressWallpaperToggle = false;
        AlarmScheduler.scheduleNext(this);
        Toast.makeText(this, enabled ? "Verse wallpaper ON — applies with your morning verse"
                : "Verse wallpaper OFF", Toast.LENGTH_SHORT).show();
    }

    private void setupTimePresets() {
        timePresetsRow.removeAllViews();
        for (int i = 0; i < PRESET_LABELS.length; i++) {
            final int h = PRESET_TIMES[i][0];
            final int m = PRESET_TIMES[i][1];
            Button b = makeChip(PRESET_LABELS[i] + " " + fmtTime(h, m));
            b.setOnClickListener(v -> saveTime(h, m));
            b.setTag(new int[]{h, m});
            timePresetsRow.addView(b);
        }
        Button custom = makeChip("Custom…");
        custom.setTag(null);
        custom.setOnClickListener(v -> {
            int hour = prefs.getInt("notif_hour", 7);
            int minute = prefs.getInt("notif_minute", 0);
            new TimePickerDialog(this, (view, h, m) -> saveTime(h, m),
                    hour, minute, false).show();
        });
        timePresetsRow.addView(custom);
        paintTimePresets();
    }

    private void paintTimePresets() {
        int hour = prefs.getInt("notif_hour", 7);
        int minute = prefs.getInt("notif_minute", 0);
        for (int i = 0; i < timePresetsRow.getChildCount(); i++) {
            Button b = (Button) timePresetsRow.getChildAt(i);
            int[] t = (int[]) b.getTag();
            boolean sel;
            if (t == null) {
                sel = !isPresetMatch(hour, minute);
                b.setText(sel ? "Custom " + fmtTime(hour, minute) : "Custom…");
            } else {
                sel = (t[0] == hour && t[1] == minute);
            }
            paintChip(b, sel);
        }
    }

    private boolean isPresetMatch(int hour, int minute) {
        for (int[] t : PRESET_TIMES) {
            if (t[0] == hour && t[1] == minute) return true;
        }
        return false;
    }

    private void saveTime(int h, int m) {
        prefs.edit().putInt("notif_hour", h).putInt("notif_minute", m).apply();
        paintTimePresets();
        AlarmScheduler.scheduleNext(this);
        Toast.makeText(this, "Verse will arrive daily at " + fmtTime(h, m),
                Toast.LENGTH_SHORT).show();
    }

    private String fmtTime(int hour, int minute) {
        String amPm = hour < 12 ? "AM" : "PM";
        int h12 = hour % 12 == 0 ? 12 : hour % 12;
        return String.format(Locale.US, "%d:%02d %s", h12, minute, amPm);
    }

    // ---------- footer ----------

    private void setupFooter() {
        privacyButton.setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyActivity.class)));
        aboutButton.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("About Sacred Verses")
                        .setMessage("One verse from the world's scriptures, every morning.\n\n"
                                + "Verses come from public-domain translations: the King James Bible, "
                                + "Edwin Arnold's Bhagavad Gita (1885), Pickthall's Qur'an (1930), "
                                + "Max Müller's Dhammapada (1881), and Macauliffe's "
                                + "The Sikh Religion (1909) — plus verses in Hindi, Sanskrit, "
                                + "Arabic, Punjabi and Pali.\n\n"
                                + "Version 4.0 · Made with care.")
                        .setPositiveButton("OK", null)
                        .show());
    }

    private void maybeShowFirstLaunch() {
        if (prefs.getBoolean("policy_seen", false)) return;
        new AlertDialog.Builder(this)
                .setTitle("Welcome to Sacred Verses")
                .setMessage("One verse from the world's scriptures, every morning.\n\n"
                        + "It arrives as a notification — and as your lockscreen wallpaper, "
                        + "so the verse is waiting when you wake your phone.\n\n"
                        + "Pick your faith in Settings to get verses from your tradition only.\n\n"
                        + "We don't collect your data — your saved verses never leave your phone.")
                .setPositiveButton("Got it", (d, w) ->
                        prefs.edit().putBoolean("policy_seen", true).apply())
                .setNeutralButton("Privacy Policy", (d, w) -> {
                    prefs.edit().putBoolean("policy_seen", true).apply();
                    startActivity(new Intent(this, PrivacyActivity.class));
                })
                .show();
    }

    // ---------- notification permission (Android 13+) ----------

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
