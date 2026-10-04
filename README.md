# Sacred Verses — Daily Wisdom (v2.5)

A native Android app that delivers one verse from the world's scriptures every
morning — as a phone notification AND as your lockscreen wallpaper, so the
verse is waiting when you wake your phone. Pick your faith in Settings to get
verses from your tradition only, or leave it on "All" and the faiths rotate —
a different tradition every day.

Three tabs: **Home** (verse of the day), **Browse** (365 unique verses — a
different one for every day of the year, filterable by Hinduism, Christianity,
Islam, Buddhism, Judaism, Sikhism), **Settings** (notification, wallpaper,
faith preference, reminder time). Save favorites, share verses.

**Every verse is from a public-domain translation** (e.g. King James Bible,
Edwin Arnold's Gita 1885, Pickthall's Qur'an 1930, JPS 1917 Tanakh, Max Müller's
Dhammapada, Macauliffe's Sikh Religion 1909), verified word-for-word against
the source texts. Each verse carries its source attribution in the app.
Zero copyright risk, zero content cost.

Total money spent to build this: **Rs 0**.

---

## Install the ready-made APK (fastest)

1. Copy `SacredVerses-v1.0.apk` to your phone (WhatsApp it to yourself,
   Bluetooth, USB — anything).
2. Tap the file → allow "Install unknown apps" when asked → Install.
3. Open the app, allow notifications. Done — tomorrow morning's verse arrives
   on your lock screen.

The APK is signed with a debug key: fine for installing and sharing
directly, but the Play Store will later need a release-signed build
(see below).

## Build it yourself (optional)

Two ways, same result:

- **On any PC with Android Studio:** open this folder as a project, let Gradle
  sync, Build → Build APK. (The Gradle files are set up and dependency-free.)
- **Command line:** `./build-manual.sh` — compiles with aapt2 + javac + d8
  straight from the bundled SDK, no Gradle daemon needed.

## Make it earn (v2.5: three paths)

### Path A — Sponsored card (works TODAY, no SDK needed) ✅

The app shows a clearly-labeled **Sponsored** card on the Home screen,
driven by `app/src/main/assets/sponsors.json`. Entries **rotate daily**
(one per day, cycling), so every sponsor gets seen. The card is labeled
"Sponsored" and the affiliate disclosure is in the app's Privacy Policy —
that's the compliant way to do it.

To earn:
1. Sign up free at [Amazon Associates](https://affiliate-program.amazon.in)
   (or any affiliate program).
2. Get your tracking tag (looks like `yourname-21`).
3. In `sponsors.json`, replace `YOURTAG-21` with your tag, and edit/add
   entries — each entry is one day's sponsor. More entries = more variety.
4. Rebuild the APK (`./build-manual.sh`) and share the new version.

You earn a commission (typically 1–10%) on every purchase made through those
links. Dismissing the card hides it only until the next day.

### Path B — "Support" button via UPI (works TODAY) ✅ wired in v2.5

A "♥ Support Sacred Verses" button appears in Settings once configured —
it opens the user's UPI app (GPay/PhonePe/Paytm) with your UPI id filled in.
Zero fees, money lands directly in your account.

To enable: open `app/src/main/assets/support.json` and replace
`REPLACE_WITH_YOUR_UPI_ID` with your UPI id (e.g. `yourname@okhdfcbank`).
Rebuild and share. The button stays hidden until a real id is set.

### Path C — AdMob banners (upgrade, needs Android Studio)

Real AdMob ads need Google's Play Services Ads SDK, which is safest added
through Gradle on a normal machine (hand-bundling it risks crashes):

1. On a PC, open this folder in Android Studio.
2. In `app/build.gradle`, add to dependencies:
   `implementation 'com.google.android.gms:play-services-ads:23.4.0'`
3. In `AndroidManifest.xml`, add inside `<application>`:
   ```xml
   <meta-data
       android:name="com.google.android.gms.ads.APPLICATION_ID"
       android:value="YOUR_ADMOB_APP_ID" />
   ```
   (Get the App ID free at [admob.com](https://admob.com).)
4. In `MainActivity.java`, initialize once and add a banner:
   ```java
   import com.google.android.gms.ads.MobileAds;
   import com.google.android.gms.ads.AdRequest;
   import com.google.android.gms.ads.AdSize;
   import com.google.android.gms.ads.AdView;
   // in onCreate:
   MobileAds.initialize(this, status -> {});
   AdView adView = new AdView(this);
   adView.setAdSize(AdSize.BANNER);
   adView.setAdUnitId("YOUR_BANNER_AD_UNIT_ID");
   ((LinearLayout) findViewById(R.id.adSlot)).addView(adView);
   adView.loadAd(new AdRequest.Builder().build());
   ```
   (Add a `LinearLayout` with id `adSlot` at the bottom of
   `activity_main.xml` first.)
5. While testing, use Google's test IDs
   (`ca-app-pub-3940256099942544~3347511713` / `.../6300978111`) —
   test ads earn nothing; swap in your real IDs before sharing.

**Honest earnings math (so there are no surprises):**
- Banner ads in India pay roughly **$0.20–$1 per 1,000 views** (eCPM).
- 100 daily users ≈ $0.05–$0.30/day → a few hundred rupees a *month*.
- 1,000 daily users ≈ $0.50–$3/day → roughly **Rs 1,200–7,500/month**.
- 10,000 daily users ≈ real money (Rs 15k+/month).
- Affiliate links pay per *purchase*, not per view — fewer users needed
  to see the first rupee.

The app doesn't earn by existing — it earns when people open it daily.
The daily notification is the engine: it brings people back every morning.
Growth ideas that cost nothing:

- Share the APK in spiritual/WhatsApp/Telegram groups (morning-forward
  culture is exactly this app's audience).
- The in-app Share button already appends "Shared via Sacred Verses app" —
  every share is free advertising.
- Later: add "share as image" (verse on a pretty background) — image forwards
  spread far faster than text.

## Go to the Play Store (later, when the app pays for it)

- One-time developer fee: **$25 (~Rs 2,100)**. Don't pay it from savings —
  let the APK version earn it first.
- When ready: in Android Studio, Build → Generate Signed Bundle/APK
  (create your own keystore, keep the password safe), upload to
  [play.google.com/console](https://play.google.com/console).

## Ideas for v2 (when you want them)

- Share-as-image cards, home-screen widget, Hindi verses,
  more faiths/texts, streak counter ("12 mornings in a row").
