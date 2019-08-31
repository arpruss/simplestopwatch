package omegacentauri.mobi.simplestopwatch;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
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
    private final Context context;
    TextView mainView1;
    TextView mainView2;
    TextView fractionView;
    public long baseTime;
    public long pauseTime;
    public boolean paused = false;
    public boolean active = false;
    TextView view;
    Timer timer;
    Handler updateHandler;
    public int precision = 100;

    public MyChrono(Context context, final TextView mainView1, final TextView mainView2, final TextView fractionView) {
        this.mainView1 = mainView1;
        this.mainView2 = mainView2;
        this.context = context;
        this.fractionView = fractionView;

        updateHandler = new Handler() {
            public void handleMessage(Message m) {
                updateViews();
            }
        };
    }

    public void updateViews() {
        long t = active ? (( paused ? pauseTime : SystemClock.elapsedRealtime() ) - baseTime) : 0;
        String line1 = formatTime(t,1);
        String line2 = formatTime(t,2);
        float s1 = maximizeSize(mainView1, line1, 0.96f);
        if (mainView2.getVisibility() == View.VISIBLE) {
            float size = Math.min(s1, maximizeSize(mainView2, line2, 0.96f));
            optionalSetSizeAndText(mainView1, size, line1);
            optionalSetSizeAndText(mainView2, size, line2);
        }
        else {
            optionalSetSizeAndText(mainView1, s1, line1);
        }
        fractionView.setText(formatTimeFraction(t));
    }

    static private void optionalSetSizeAndText(TextView v, float newSize, String text) {
        if (Math.abs(newSize - v.getTextSize()) > 10)
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);
        if (! v.getText().equals(text))
            v.setText(text);

    }

    String formatTime(long t, int line) {
        t /= 1000;
        int s = (int) (t % 60);
        t /= 60;
        int m = (int) (t % 60);
        t /= 60;
        int h = (int) t;
        if (mainView2.getVisibility() == View.VISIBLE) {
            if (line == 1) {
                if (h != 0)
                    return String.format("%d:%02d", h, m);
                else
                    return String.format("%d", m);
            }
            else {
                return String.format("%02d", s);
            }
        }
        else {
            if (line == 1) {
                if (h != 0)
                    return String.format("%d:%02d:%02d", h, m, s);
                else
                    return String.format("%d:%02d", m, s);
            }
            else {
                return "";
            }
        }
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

    private static float maximizeSize(TextView v, String text, float scale) {
        float curSize = v.getTextSize();
        if (text.length() == 0)
            return curSize;
        float vWidth = v.getWidth();
        float vHeight = v.getHeight();
        if (vWidth == 0 || vHeight == 0)
            return curSize;
        String s = text.replaceAll("[0-9]", "8");
        Paint p = new Paint(v.getPaint());
        p.setTextSize(50);
        Rect bounds = new Rect();
        p.getTextBounds((String)s, 0, s.length(), bounds);
        float textWidth = bounds.width();
        float textHeight = bounds.height();
        if (textWidth == 0 || textHeight == 0)
            return curSize;

        float resize = Math.min(vWidth/textWidth, vHeight/textHeight);
        return (float)(p.getTextSize()*scale*resize);
    }

    public void resetButton() {
        if (! paused)
            return;
        stopUpdating();
        active = false;
        updateViews();
    }

    public void startStopButton() {
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

    public void restore(SharedPreferences pref, String prefix) {
        baseTime = pref.getLong(prefix+PREFS_START_TIME, 0);
        pauseTime = pref.getLong(prefix+PREFS_PAUSED_TIME, 0);
        active = pref.getBoolean(prefix+PREFS_ACTIVE, false);
        paused = pref.getBoolean(prefix+PREFS_PAUSED, false);
        if (SystemClock.elapsedRealtime() <= baseTime)
            active = false;

        if (active && !paused) {
            startUpdating();
        }
        else {
            stopUpdating();
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
        if (timer != null) {
            Log.v("chrono", "stop update");
            timer.cancel();
            timer = null;
        }
        ((Activity)context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void startUpdating() {
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    updateHandler.obtainMessage(1).sendToTarget();
                }
            }, 0, precision);
        }
        ((Activity)context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static void clearSaved(SharedPreferences pref, String prefix) {
        SharedPreferences.Editor ed = pref.edit();
        ed.putBoolean(prefix+PREFS_ACTIVE, false);
        ed.apply();
        Log.v("chrono", "cleared "+prefix+PREFS_ACTIVE);
    }

    public static void detectBoot(SharedPreferences options, String prefix) {
        return;/*
        long bootTime = java.lang.System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime();
        long oldBootTime = options.getLong(prefix+PREFS_BOOT_TIME, -100000);
        SharedPreferences.Editor ed = options.edit();
        if (Math.abs(oldBootTime-bootTime)>60000) {
            ed.putBoolean(prefix+PREFS_ACTIVE, false);
        }
        ed.putLong(prefix+PREFS_BOOT_TIME, bootTime);
        ed.apply(); */
    }
}
