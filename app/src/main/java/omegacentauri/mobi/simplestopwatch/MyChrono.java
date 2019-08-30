package omegacentauri.mobi.simplestopwatch;

import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Float.max;

public class MyChrono {
    private static final String PREFS_START_TIME = "baseTime";
    private static final String PREFS_PAUSED_TIME = "pausedTime";
    private static final String PREFS_ACTIVE = "active";
    private static final String PREFS_PAUSED = "paused";
    private static final String PREFS_BOOT_TIME = "boot";
    TextView mainView;
    TextView fractionView;
    public long baseTime;
    public long pauseTime;
    public boolean paused = false;
    public boolean active = false;
    TextView view;
    Timer timer;
    Handler updateHandler;
    public int precision = 100;

    public MyChrono(final TextView mainView, final TextView fractionView) {
        this.mainView = mainView;
        this.fractionView = fractionView;

        updateHandler = new Handler() {
            public void handleMessage(Message m) {
                updateViews();
            }
        };

        timer = new Timer();
    }

    public void updateViews() {
        long t = active ? (( paused ? pauseTime : SystemClock.elapsedRealtime() ) - baseTime) : 0;
        String main = formatTime(t);
        String fraction = formatTimeFraction(t);
        MyChrono.this.mainView.setText(main);
        maximizeSize(MyChrono.this.mainView);
        MyChrono.this.fractionView.setText(fraction);
    }

    static String formatTime(long t) {
        t /= 1000;
        int s = (int) (t % 60);
        t /= 60;
        int m = (int) (t % 60);
        t /= 60;
        int h = (int) t;
        if (h != 0)
            return String.format("%d:%02d:%02d", h, m, s);
        else
            return String.format("%02d:%02d", m, s);
    }

    String formatTimeFraction(long t) {
        if (precision == 100)
            return String.format(".%01d", (int)((t / 100) % 10));
        else if (precision == 10)
            return String.format(".%02d", (int)((t / 10) % 100));
        else if (precision == 1)
            return String.format(".%03d", (int)(t % 1000));
        else
            return "";
    }

    public static void maximizeSize(TextView v) {
        String s = ((String)v.getText()).replaceAll("[0-9]", "8");
        if (s.length() == 0)
            return;
        Paint p = new Paint(v.getPaint());
        p.setTextSize(50);
        Rect bounds = new Rect();
        p.getTextBounds((String)s, 0, s.length(), bounds);
        float textWidth = bounds.width();
        float textHeight = bounds.height();

        if (v.getWidth() > 0 && v.getHeight() > 0) {
            try {
                float scale = Math.min(v.getWidth() / (float)bounds.width(), v.getHeight() / (float)bounds.height());
                float newSize = (float) (p.getTextSize()*scale*0.9);
                if (Math.abs(newSize - v.getTextSize()) > 10) {
                    v.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);
                }
            }
            catch(Exception e) {}
        }
    }

    public void reset() {
        if (! paused)
            return;
        stopUpdating();
        active = false;
    }

    public void start() {
        if (active && paused) {
            baseTime += SystemClock.elapsedRealtime() - pauseTime;
            paused = false;
            startUpdating();
        }
        else if (!active) {
            baseTime = SystemClock.elapsedRealtime();
            paused = false;
            active = true;
            startUpdating();
        }
        else {
            paused = true;
            pauseTime = SystemClock.elapsedRealtime();
            stopUpdating();
        }
        updateViews();
    }

    public void pause() {
        stopUpdating();
        pauseTime = SystemClock.elapsedRealtime();
        paused = true;
        updateViews();
    }

    public void resume() {
    }

    public void restore(SharedPreferences pref, String prefix) {
        baseTime = pref.getLong(prefix+PREFS_START_TIME, 0);
        pauseTime = pref.getLong(prefix+PREFS_PAUSED_TIME, 0);
        active = pref.getBoolean(prefix+PREFS_ACTIVE, false);
        paused = pref.getBoolean(prefix+PREFS_PAUSED, false);

        if (active && !paused) {
            startUpdating();
        }
        updateViews();
    }

    public void save(SharedPreferences pref, String prefix) {
        SharedPreferences.Editor ed = pref.edit();
        ed.putLong(prefix+PREFS_START_TIME, baseTime);
        ed.putLong(prefix+PREFS_PAUSED_TIME, pauseTime);
        ed.putBoolean(prefix+PREFS_ACTIVE, active);
        ed.putBoolean(prefix+PREFS_PAUSED, paused);
        ed.apply();
    }

    public void stopUpdating() {
        timer.purge();
    }

    public void startUpdating() {
        timer.purge();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                updateHandler.obtainMessage(1).sendToTarget();
            }
        }, 0, precision);
    }

    public static void clearSaved(SharedPreferences pref, String prefix) {
        SharedPreferences.Editor ed = pref.edit();
        ed.putBoolean(prefix+PREFS_ACTIVE, false);
        ed.apply();
    }

    public static void detectBoot(SharedPreferences options, String prefix) {
        long bootTime = java.lang.System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime();
        long oldBootTime = options.getLong(prefix+PREFS_BOOT_TIME, -100000);
        SharedPreferences.Editor ed = options.edit();
        if (Math.abs(oldBootTime-bootTime)>60000) {
            ed.putBoolean(prefix+PREFS_ACTIVE, false);
        }
        ed.putLong(prefix+PREFS_BOOT_TIME, bootTime);
        ed.apply();
    }
}
