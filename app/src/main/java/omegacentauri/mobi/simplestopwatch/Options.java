package omegacentauri.mobi.simplestopwatch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.Html;
import android.util.Log;

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
    public static final String PREF_COLOR = "color";
    public static final String PREF_ORIENTATION = "orientation";
    public static final String PREF_FORMAT = "format";
    public static final String PREF_FONT = "font";
    public static final String PREF_THREE_LINE = "threeLine";
    public static final String PREF_KEEP_ASPECT = "keepAspect";
    public static final String PREF_LINE_SPACING = "lineSpacing";
    public static final String PREF_LETTER_SPACING = "letterSpacing";
    public static final String PREF_LAST_LAP_TIME = "lastLapTime";
    public static final String PREF_DELAY = "delay";
    public static final String PREF_SCALE = "scale";
    public static final String PREF_LAPS = "laps";
    public static final String PREF_LAST_ANNOUNCED = "lastAnnounced";
    public static final String PREF_SOUND = "sounds";
    public static final String PREF_BOOST = "boost";
    public static final String PREF_ALARM = "alarm";
    public static final String PREF_VOLUME = "volume";
    public static final String PREF_FULLSCREEN = "fullscreen";
    public static final int highlightPercent = 25;
    public static final String PREF_BOOT_ADJUSTED = "bootAdjusted";
    static Map<String, int[]> colorMap = new HashMap<String,int[]>();
    static final int[] defaultColor = {Color.WHITE, Color.BLACK};

    private static void addColor(String name, int fg, int bg) {
        colorMap.put(name, new int[]{fg,bg});
    }
    static {
        addColor("black on white", Color.BLACK, Color.WHITE);
        addColor("green on black", Color.GREEN, Color.BLACK);
        addColor("red on black", Color.RED, Color.BLACK);
        addColor("yellow on black", Color.YELLOW, Color.BLACK);
        addColor("white on black", Color.WHITE, Color.BLACK);
    }

    static MiniFont getFont(SharedPreferences options) {
        String f = options.getString(PREF_FONT, "medium");
        if (f.equals("regular")) {
            Log.v("chrono", "regular");
            return new SansDigitsColon();
        }
        else if (f.equals("7 segment")) {
            return new SevenSegmentBoldItalicDigitsColon();
        }
        else if (f.equals("DIN 1451")) {
            return new DINDigitsColon();
        }
        else if (f.equals("medium")) {
            return new SansMediumDigitsColon();
        }
        else {
            Log.v("chrono", "bold");
            return new SansBoldDigitsColon();
        }
    }

    static int getForeColor(SharedPreferences options) {
        try {
            return colorMap.get(options.getString(PREF_COLOR, "white on black"))[0];
        }
        catch(Exception e) {
            return defaultColor[0];
        }
    }

    static int getBackColor(SharedPreferences options) {
        try {
            return colorMap.get(options.getString(PREF_COLOR, "white on black"))[1];
        }
        catch(Exception e) {
            return defaultColor[1];
        }
    }

    static int getHighlightColor(SharedPreferences options) {
        int fore = getForeColor(options);
        int back = getBackColor(options);

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
        addPreferencesFromResource(R.xml.options);

        Preference lb = (Preference) findPreference("license");
        lb.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showLicenses();

                return false;
            }
        });

        //getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
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
