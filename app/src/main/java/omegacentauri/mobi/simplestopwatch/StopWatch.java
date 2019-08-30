package omegacentauri.mobi.simplestopwatch;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Chronometer;

public class StopWatch extends Activity {
    private static final String PREFS_START_TIME = "baseTime";
    private static final String PREFS_PAUSED_TIME = "pausedTime";
    private static final String PREFS_ACTIVE = "active";
    private static final String PREFS_PAUSED = "paused";
    SharedPreferences options;
    long baseTime = 0;
    long pausedTime = 0;
    boolean active = false;
    boolean paused = false;
    boolean chronoStarted = false;
    private Chronometer chrono;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        options = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_stop_watch);
        chrono = (Chronometer)findViewById(R.id.chrono);
        Log.v("chrono", ""+(chrono!=null));
        chrono.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                                             @Override
                                             public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                                                 MyChrono.maximizeSize(chrono);
                                             }
                                         }
        );

    }

    @Override
    protected void onResume() {
        super.onResume();

        baseTime = options.getLong(PREFS_START_TIME, 0);
        pausedTime = options.getLong(PREFS_PAUSED_TIME, 0);
        active = options.getBoolean(PREFS_ACTIVE, false);
        paused = options.getBoolean(PREFS_PAUSED, false);
        chrono.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                MyChrono.maximizeSize(chrono);
            }
        });

        if (active) {
            setParams();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    String formatTime(long t) {
        t /= 1000;
        int s = (int) (t % 60);
        t /= 60;
        int m = (int) (t % 60);
        t /= 60;
        int h = (int) t;
        if (h != 0)
            return String.format("%d:%02d:%02d", h,m,s);
        else
            return String.format("%02d:%02d", m,s);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (chronoStarted) {
            chrono.stop();
            chronoStarted = false;
        }
    }

    void setParams() {
        if (!active) {
            if (chronoStarted) {
                chrono.stop();
                chronoStarted = false;
            }
            chrono.setText(formatTime(0));
        }
        else {
            if (paused) {
                if (chronoStarted) {
                    chrono.stop();
                    chronoStarted = false;
                }
                chrono.setText(formatTime(pausedTime - baseTime));
            } else {
                if (chronoStarted)
                    chrono.stop();
                chrono.setBase(baseTime);
                chrono.start();
                chronoStarted = true;
            }
        }

        MyChrono.maximizeSize(chrono);

        SharedPreferences.Editor ed = options.edit();
        ed.putLong(PREFS_START_TIME, baseTime);
        ed.putLong(PREFS_PAUSED_TIME, pausedTime);
        ed.putBoolean(PREFS_ACTIVE, active);
        ed.putBoolean(PREFS_PAUSED, paused);
        ed.commit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN)
            return false;
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_A) {
            if (active) {
                if (!paused) {
                    paused = true;
                    pausedTime = SystemClock.elapsedRealtime();
                    setParams();
                }
                else {
                    paused = false;
                    baseTime += SystemClock.elapsedRealtime() - pausedTime;
                    setParams();
                }
            }
            else {
                paused = false;
                baseTime = SystemClock.elapsedRealtime();
                active = true;
                setParams();
            }
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_C) {
            if (active && paused) {
                active = false;
                setParams();
            }
            return true;
        }
        return false;
    }
}
