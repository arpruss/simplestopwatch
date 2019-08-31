package omegacentauri.mobi.simplestopwatch;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import java.util.HashMap;
import java.util.Map;

public class Options extends PreferenceActivity {
    public static final String PREFS_START_TIME = "baseTime";
    public static final String PREFS_PAUSED_TIME = "pausedTime";
    public  static final String PREFS_ACTIVE = "active";
    public static final String PREFS_PAUSED = "paused";
    public static final String PREFS_BOOT_TIME = "boot";
    public static final String PREFS_SCREEN_ON = "screenOn";
    public static final String PREFS_PRECISION = "precision";
    public static final String PREFS_COLOR = "color";
    static Map<String, int[]> colorMap = new HashMap<String,int[]>();
    private static void addColor(String name, int fg, int bg) {
        colorMap.put(name, new int[]{fg,bg});
    }
    static {
        addColor("black on white", Color.BLACK, Color.WHITE);
        addColor("white on black", Color.WHITE, Color.BLACK);
        addColor("green on black", Color.GREEN, Color.BLACK);
        addColor("red on black", Color.RED, Color.BLACK);
    }

    static int getForeColor(SharedPreferences options) {
        return colorMap.get(options.getString(PREFS_COLOR, "white on black"))[0];
    }

    static int getBackColor(SharedPreferences options) {
        return colorMap.get(options.getString(PREFS_COLOR, "white on black"))[1];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.options);
        //getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
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
