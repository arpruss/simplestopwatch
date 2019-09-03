package omegacentauri.mobi.simplestopwatch;
//
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MyChrono {
    private final Activity context;
    BigTextView mainView;
    TextView fractionView;
    public long baseTime;
    public long pauseTime;
    public long delayTime;
    static final long MIN_DELAY_TIME = -9000;
    static final long delayTimes[] = { 0, -3000, -6000, MIN_DELAY_TIME };
    public boolean paused = false;
    public boolean active = false;
    TextView view;
    Timer timer;
    int maxSize;
    Handler updateHandler;
    SharedPreferences options;
    public int precision = 100;

    @SuppressLint("NewApi")
    public MyChrono(Activity context, SharedPreferences options, BigTextView mainView, TextView fractionView) {
        this.mainView = mainView;
        this.context = context;
        this.options = options;
        this.delayTime = options.getLong(Options.PREF_DELAY, 0);
        this.fractionView = fractionView;
        Log.v("chrono", "maxSize " +this.maxSize);

        updateHandler = new Handler() {
            public void handleMessage(Message m) {
                updateViews();
            }
        };
    }

    public long getTime() {
        return active ? (( paused ? pauseTime : SystemClock.elapsedRealtime() ) - baseTime) : delayTime;
    }

    public void updateViews() {
        long t = getTime();
        mainView.setText(formatTime(t,mainView.getHeight() > mainView.getWidth()));
        fractionView.setText(formatTimeFraction(t));
    }

    static private void optionalSetSizeAndText(TextView v, float newSize, String text) {
        if (Math.abs(newSize - v.getTextSize()) > 10)
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);
        if (! v.getText().equals(text))
            v.setText(text);

    }

    static final long floorDiv(long a, long b) {
        long q = a/b;
        if (q*b > a)
            return q-1;
        else
            return q;
    }

    String formatTime(long t, boolean multiline) {
        //t+=1000*60*60*60;
        if (t<0)
            return String.format("\u2212%d", -floorDiv(t,1000));
        String format = options.getString(Options.PREF_FORMAT, "h:m:s");
        t /= 1000;
        if (format.equals("s")) {
            return String.format("%02d", t);
        }
        int s = (int) (t % 60);
        t /= 60;
        int m;
        int h;
        if (format.equals("h:m:s")) {
            m = (int) (t % 60);
            t /= 60;
            h = (int) t;
        }
        else {
            m = (int) t;
            h = 0;
        }
        if (multiline) {
            Boolean threeLine = options.getBoolean(Options.PREF_THREE_LINE, true);
            if (h != 0) {
                if (threeLine)
                    return String.format("%02d\n%02d\n%02d", h, m, s);
                else
                    return String.format("%d:%02d\n%02d", h, m, s);
            }
            else
                return String.format("%02d\n%02d", m, s);
        }
        else {
            if (h != 0)
                return String.format("%d:%02d:%02d", h, m, s);
            else
                return String.format("%d:%02d", m, s);
        }
    }

    String formatTimeFraction(long t) {
        if (t<0)
            return "";
        if (precision == 10 || (precision > 10 && active && paused))
            return String.format(".%02d", (int)((t / 10) % 100));
        else if (precision == 100)
            return String.format(".%01d", (int)((t / 100) % 10));
        else if (precision == 1)
            return String.format(".%03d", (int)(t % 1000));
        else
            return "";
    }

    public void resetButton() {
        if (! paused)
            return;
        if (active) {
            active = false;
            stopUpdating();
        }
        else {
            int i = 0;
            for (i = 0; i < delayTimes.length; i++) {
                if (delayTimes[i] == delayTime) {
                    i = (i+1) % delayTimes.length;
                    break;
                }
            }
            delayTime = delayTimes[i];
        }
        save();
        updateViews();
    }

    public void startStopButton() {
        if (active && paused) {
            baseTime += SystemClock.elapsedRealtime() - pauseTime;
            paused = false;
            startUpdating();
            save();
        }
        else if (!active) {
            baseTime = SystemClock.elapsedRealtime() - delayTime;
            paused = false;
            active = true;
            startUpdating();
            save();
        }
        else {
            paused = true;
            pauseTime = SystemClock.elapsedRealtime();
            stopUpdating();
            save();
        }
        updateViews();
    }

    public void restore() {
        baseTime = options.getLong(Options.PREFS_START_TIME, 0);
        pauseTime = options.getLong(Options.PREFS_PAUSED_TIME, 0);
        delayTime = options.getLong(Options.PREF_DELAY, 0);
        active = options.getBoolean(Options.PREFS_ACTIVE, false);
        paused = options.getBoolean(Options.PREFS_PAUSED, false);
        precision = Integer.parseInt(options.getString(Options.PREFS_PRECISION, "100"));
        if (SystemClock.elapsedRealtime() < baseTime + MIN_DELAY_TIME)
            active = false;

        if (active && !paused) {
            startUpdating();
        }
        else {
            stopUpdating();
        }
        updateViews();
    }

    public void save() {
        SharedPreferences.Editor ed = options.edit();
        ed.putLong(Options.PREFS_START_TIME, baseTime);
        ed.putLong(Options.PREFS_PAUSED_TIME, pauseTime);
        ed.putBoolean(Options.PREFS_ACTIVE, active);
        ed.putBoolean(Options.PREFS_PAUSED, paused);
        ed.putLong(Options.PREF_DELAY, delayTime);
        ed.apply();
    }

    public void stopUpdating() {
        if (timer != null) {
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
            }, 0, precision<=10 ? precision : 50); // avoid off-by-1 errors at lower precisions, at cost of some battery life
        }
        if (options.getBoolean(Options.PREFS_SCREEN_ON, false))
            ((Activity)context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            ((Activity)context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static void clearSaved(SharedPreferences pref) {
        SharedPreferences.Editor ed = pref.edit();
        ed.putBoolean(Options.PREFS_ACTIVE, false);
        ed.apply();
        Log.v("chrono", "cleared "+Options.PREFS_ACTIVE);
    }

    public static void detectBoot(SharedPreferences options) {
        return;
        /*
        long bootTime = java.lang.System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime();
        long oldBootTime = options.getLong(Options.PREFS_BOOT_TIME, -100000);
        SharedPreferences.Editor ed = options.edit();
        if (Math.abs(oldBootTime-bootTime)>60000) {
            ed.putBoolean(Options.PREFS_ACTIVE, false);
        }
        ed.putLong(Options.PREFS_BOOT_TIME, bootTime);
        ed.apply(); */
    }

    public void copyToClipboard() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            long t = getTime();
            String s = formatTime(t, false) + formatTimeFraction(t);
            ClipboardManager clip = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData data = ClipData.newPlainText("time", s);
            clip.setPrimaryClip(data);
//            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT);
        }
    }
}
