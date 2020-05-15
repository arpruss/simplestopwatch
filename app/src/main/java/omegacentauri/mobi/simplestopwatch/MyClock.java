package omegacentauri.mobi.simplestopwatch;
//
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MyClock implements BigTextView.GetCenter, MyTimeKeeper {
    private final Activity context;
    private final View mainContainer;
    protected final SharedPreferences options;
    private final Handler updateHandler;
    BigTextView mainView;
    TextView fractionView;
    private Timer timer;
    private final int precision = 1000;
    SimpleDateFormat timeFormat;
    SimpleDateFormat fractionalFormat;
    DateFormat dateFormat;
    protected boolean twentyFourHour = false;

    @SuppressLint("NewApi")
    public MyClock(Activity context, SharedPreferences options, BigTextView mainView, TextView fractionView, View mainContainer) {
        this.mainView = mainView;
        this.context = context;
        this.options = options;
        this.mainContainer = mainContainer;
        this.fractionView = fractionView;
        this.mainView.getCenterY = this;

        updateHandler = new Handler() {
            public void handleMessage(Message m) {
                updateViews();
            }
        };
    }

    public void updateViews() {
        Date date = new Date();
        mainView.setText(getTimeMain(date, false));
        setFractionView(getTimeFraction(date));
    }

    private String getTimeMain(Date d, boolean clip) {
        String t = timeFormat.format(d);
        if (!clip && mainView.getHeight() > mainView.getWidth()) {
            if (t.length() == 4 || t.length() == 7)
                t = " " + t;
            return t.replaceAll(":", "\n");
        }
//        else if (!clip && d.getSeconds() % 2 == 1)
//            return t.replace(":", "|");
        else
            return t;
    }

    private String getTimeFraction(Date d) {
        return fractionalFormat.format(d);
    }

    private String getTimeDate(Date d) {
        return dateFormat.format(d);
    }

    private void setFractionView(String s) {
        fractionView.setText(s);
        fractionView.setTextScaleX(1f);
        float w = fractionView.getPaint().measureText(s, 0, s.length());
        float wCur = fractionView.getWidth() * 0.98f - 2;
        if (wCur <= 0) {
            fractionView.setText("");
            return;
        }
        if (w > wCur)
            fractionView.setTextScaleX(wCur/w);
        fractionView.setText(s);
    }

    static final long floorDiv(long a, long b) {
        long q = a/b;
        if (q*b > a)
            return q-1;
        else
            return q;
    }

    public void restore() {
        twentyFourHour = options.getBoolean(Options.PREF_24HOUR, false);
        if (!twentyFourHour) {
            timeFormat = new SimpleDateFormat("h:mm");
            fractionalFormat = new SimpleDateFormat(":ss a");
        }
        else {
            timeFormat = new SimpleDateFormat("HH:mm");
            fractionalFormat = new SimpleDateFormat(":ss");
        }
        dateFormat = DateFormat.getDateInstance();
        startUpdating();
        updateViews();
    }

    public static void apply(SharedPreferences.Editor ed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            ed.apply();
        }
        else {
            ed.commit();
        }
    }

    public static long getBootTime() {
        return System.currentTimeMillis() - SystemClock.elapsedRealtime();
    }

    public void save() {
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
        if (options.getBoolean(Options.PREF_SCREEN_ON, true))
            ((Activity)context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            ((Activity)context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static void clearSaved(SharedPreferences pref) {
        SharedPreferences.Editor ed = pref.edit();
        ed.putBoolean(Options.PREF_ACTIVE, false);
        apply(ed);
        StopWatch.debug("cleared "+Options.PREF_ACTIVE);
    }

    public static void detectBoot(SharedPreferences options) {
        return;
    }

    public void destroy() {
    }

    @Override
    public void suspend() {
    }

    public static void fixOnBoot(SharedPreferences options) {
        if (! options.getBoolean(Options.PREF_ACTIVE, false)) {
            StopWatch.debug("not active");
            return;
        }
        long oldBootTime = options.getLong(Options.PREF_BOOT_TIME, 0);
        SharedPreferences.Editor ed = options.edit();
        if (oldBootTime == 0) {
            ed.putBoolean(Options.PREF_ACTIVE, false);
        }
        else {
            long delta = getBootTime() - oldBootTime;
            if (delta == 0)
                return;
            adjust(options, ed, Options.PREF_START_TIME, -delta);
            adjust(options, ed, Options.PREF_PAUSED_TIME, -delta);
            ed.putBoolean(Options.PREF_BOOT_ADJUSTED, true);
        }
        apply(ed);
    }

    private static void adjust(SharedPreferences options, SharedPreferences.Editor ed, String opt, long delta) {
        StopWatch.debug("opt "+opt+" "+delta);
        ed.putLong(opt, options.getLong(opt, 0) + delta);
    }

    @Override
    public float getCenter() {
        return mainContainer.getHeight() / 2f;
    }

    public void copyToClipboard() {
        ShowTime.clip(context, getTimeMain(new Date(),true));
    }
}
