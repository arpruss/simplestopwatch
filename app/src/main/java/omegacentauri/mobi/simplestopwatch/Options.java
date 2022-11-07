package omegacentauri.mobi.simplestopwatch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Options extends PreferenceActivity {
    public static final String PREF_START_TIME = "baseTime";
    public static final String PREF_PAUSED_TIME = "pausedTime";
    public static final String PREF_ACTIVE = "active";
    public static final String PREF_PAUSED = "paused";
    public static final String PREF_BOOT_TIME = "boot";
    public static final String PREF_SCREEN_ON = "screenOn";
    public static final String PREF_PRECISION = "precision";
    public static final String PREF_STOPWATCH_COLOR = "color";
    public static final String PREF_CLOCK_COLOR = "clockColor";
    public static final String PREF_ORIENTATION = "orientation";
    public static final String PREF_FORMAT = "format";
    public static final String PREF_FONT = "font";
    public static final String PREF_THREE_LINE = "threeLine";
    public static final String PREF_DISTORTION = "distortion";
    public static final String PREF_LINE_SPACING = "lineSpacing";
    public static final String PREF_LETTER_SPACING = "letterSpacing";
    public static final String PREF_LAST_LAP_TIME = "lastLapTime";
//    public static final String PREF_VIBRATION = "vibration";
    public static final String PREF_DELAY = "delay";
    public static final String PREF_SCALE = "scale";
    public static final String PREF_LAPS = "laps";
    public static final String PREF_LAST_ANNOUNCED = "lastAnnounced";
    public static final String PREF_SOUND = "sounds";
    public static final String PREF_BOOST = "boost";
    public static final String PREF_ALARM = "alarm";
    public static final String PREF_VOLUME = "volume";
    public static final String PREF_FULLSCREEN = "fullscreen";
    public static final String PREF_FS_BRIGHT = "fsBright";
    public static final String PREF_VIBRATE_AFTER_COUNTDOWN = "vibrateAfterCountdown";
    //public static final String PREF_CONTROL_FULLSCREEN = "controlFS";
    public static final String PREF_TAP_ACTION = "tapAction";
    public static final String PREF_BOOT_ADJUSTED = "bootAdjusted";
    public static final String PREF_24HOUR = "twentyfour";
    public static final String PREF_CLOCK_SWIPE_INFO = "clockSwipeInfo2";
    public static final String PREF_STOPWATCH_SWIPE_INFO = "stopwatchSwipeInfo2";
//    public static final String PREF_SWIPE = "swipe";
    public static final String PREF_CLOCK_BIG_SECONDS = "clockWithBigSeconds";
    public static final String PREF_CLOCK_LITTLE_SECONDS = "clockWithLittleSeconds";
    public static final int highlightPercent = 25;
    public static final String PREF_LAST_ACTIVITY = "lastActivity";
    public static final String PREF_PIN_ON_LOCK = "pinOnLock";
    static Map<String, int[]> colorMap = new HashMap<String,int[]>();
    static final int[] defaultColor = {Color.WHITE, Color.BLACK};
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private SharedPreferences options;
//    private SharedPreferences options;

    private static void addColor(String name, int fg, int bg) {
        colorMap.put(name, new int[]{fg,bg});
    }
    static {
        addColor("gray (20%) on black", Color.argb(255,20*255/100,20*255/100,20*255/100), Color.BLACK);
        addColor("gray (40%) on black", Color.argb(255,40*255/100,40*255/100,40*255/100), Color.BLACK);
        addColor("gray (60%) on black", Color.argb(255,60*255/100,60*255/100,60*255/100), Color.BLACK);
        addColor("gray (80%) on black", Color.argb(255,80*255/100,80*255/100,80*255/100), Color.BLACK);
        addColor("black on white", Color.BLACK, Color.WHITE);
        addColor("dark green on black", Color.argb(255,0,128,0), Color.BLACK);
        addColor("green on black", Color.GREEN, Color.BLACK);
        addColor("red on black", Color.RED, Color.BLACK);
        addColor("yellow on black", Color.YELLOW, Color.BLACK);
        addColor("white on black", Color.WHITE, Color.BLACK);
    }

    static String getTapAction(SharedPreferences options) {
        return options.getString(PREF_TAP_ACTION, "fullscreen");
    }

    static boolean swipeEnabled(SharedPreferences options) {
        return !getTapAction(options).equals("start_stop") && (
                options.getBoolean(PREF_CLOCK_LITTLE_SECONDS, true) || options.getBoolean(PREF_CLOCK_BIG_SECONDS, true) );
    }


    static float getMaxAspect(SharedPreferences options) {
        return Float.parseFloat(options.getString(Options.PREF_DISTORTION, "10"))*.01f + 1f;
    }

    static MiniFont getFont(SharedPreferences options) {
        String f = options.getString(PREF_FONT, "medium");
        MiniFont mf;
        if (f.equals("regular")) {
            Log.v("chrono", "regular");
            mf = new SansDigitsColon();
        }
        else if (f.equals("7 segment")) {
            mf = new SevenSegmentBoldItalicDigitsColon();
        }
        else if (f.equals("DIN 1451")) {
            mf = new DINDigitsColon();
        }
        else if (f.equals("medium")) {
            mf = new SansMediumDigitsColon();
        }
        else if (f.equals("black")) {
            mf = new SansBlackDigitsColon();
        }
        else {
            mf = new SansBoldDigitsColon();
        }

/*        MiniFont.Glyph colon = mf.map.get(':');
        mf.addCharacter('|', colon.width, colon.lsb, new MiniFont.PathMaker(){
            @Override
            public Path makePath() {
                return new Path();
            }
        }); */

        return mf;
    }

    static int getForeColor(ShowTime st, SharedPreferences options) {
        try {
            return colorMap.get(options.getString(st.colorThemeOptionName, "white on black"))[0];
        }
        catch(Exception e) {
            return defaultColor[0];
        }
    }

    static int getBackColor(ShowTime st, SharedPreferences options) {
        try {
            return colorMap.get(options.getString(st.colorThemeOptionName, "white on black"))[1];
        }
        catch(Exception e) {
            return defaultColor[1];
        }
    }

    static int getHighlightColor(ShowTime st, SharedPreferences options) {
        int fore = getForeColor(st, options);
        int back = getBackColor(st, options);

        int high = 0;
        for (int i = 0 ; i < 4 ; i++) {
            int shift = i*8;
            int mask = 0xFF << (i*8);
            int f = (fore >> shift) & 0xFF;
            int b = (back >> shift) & 0xFF;
            int c = (b * highlightPercent + f * (100-highlightPercent)) / 100;
            high |= c << shift;
        }

        return high;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.options, true);

        options = PreferenceManager.getDefaultSharedPreferences(this);

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                StopWatch.debug("changed pref1");
                customizeDisplay();
            }
        };

        addPreferencesFromResource(R.xml.options);
        customizeDisplay();

        Preference lb = (Preference) findPreference("license");
        lb.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showLicenses();

                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        StopWatch.debug("Options::onResume");
        
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
        customizeDisplay();
    }

    static public String getAssetFile(Context context, String assetName) {
        try {
            return getStreamFile(context.getAssets().open(assetName));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return "";
        }
    }

    static private String getStreamFile(InputStream stream) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(stream));

            String text = "";
            String line;
            while (null != (line=reader.readLine()))
                text = text + line;
            return text;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return "";
        }
    }

    public void showLicenses() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Licenses and copyrights");
        alertDialog.setMessage(Html.fromHtml(getAssetFile(this, "license.html")));
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {} });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {} });
        alertDialog.show();
    }

    public static int getStream(SharedPreferences options) {
        if (options.getBoolean(Options.PREF_ALARM, true))
            return AudioManager.STREAM_ALARM;
        else
            return AudioManager.STREAM_MUSIC;
    }

    private void scanPreferences(PreferenceGroup group) {
        int n = group.getPreferenceCount();
        for (int i=0; i <n; i++) {
            Preference p = group.getPreference(i);
            if (p instanceof PreferenceGroup) {
                scanPreferences((PreferenceGroup)p);
            }
            else {
                if (p instanceof ListPreference) {
                    setSummary((ListPreference)p);
                }
            }
        }
    }

    private void customizeDisplay() {
        StopWatch.debug("customizing option display");
        scanPreferences(getPreferenceScreen());
        boolean swipeWorks = !options.getString(PREF_TAP_ACTION, "fullscreen").equals("start_stop");
        findPreference(PREF_CLOCK_BIG_SECONDS).setEnabled(swipeWorks);
        findPreference(PREF_CLOCK_LITTLE_SECONDS).setEnabled(swipeWorks);
    }

    public void setSummary(ListPreference p) {
        try {
            p.setSummary(p.getEntry().toString().replace("%", "\uFF05")); // fullwidth percent symbol, won't be interpreted as formatting
        }
        catch(Exception e) {
            p.setSummary("");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getScanCode() == 513 || event.getScanCode() == 595) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
                finish();
            }
            else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


//    public static int getVibration(SharedPreferences options) {
//        String v = options.getString(PREF_VIBRATION, "20");
//        return Integer.parseInt(v);
//    }

/*    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    } */
}
