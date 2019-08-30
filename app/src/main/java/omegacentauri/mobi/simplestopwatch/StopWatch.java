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
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

public class StopWatch extends Activity {
    SharedPreferences options;
    long baseTime = 0;
    long pausedTime = 0;
    boolean active = false;
    boolean paused = false;
    boolean chronoStarted = false;
    private TextView chrono = null;
    private MyChrono stopwatch;
    private Button resetButton;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        options = PreferenceManager.getDefaultSharedPreferences(this);
        MyChrono.detectBoot(options, "");
        setContentView(R.layout.activity_stop_watch);
        chrono = (TextView)findViewById(R.id.chrono);
        resetButton = (Button)findViewById(R.id.reset);
        startButton = (Button)findViewById(R.id.start);
        stopwatch = new MyChrono(chrono, (TextView)findViewById(R.id.fraction));
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

        MyChrono.detectBoot(options, "");
        stopwatch.restore(options, "");
        updateButtons();
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
        stopwatch.save(options, "");
        stopwatch.stopUpdating();
    }
/*
    void setParams() {
        if (chrono == null)
            return;
        if (!active) {
            if (chronoStarted) {
                stopwatch.stop();
                chronoStarted = false;
            }
            chrono.setText(formatTime(0));
            startButton.setText("Start");
            resetButton.setVisibility(View.INVISIBLE);
        }
        else {
            if (paused) {
                if (chronoStarted) {
                    stopwatch.stop();
                    chronoStarted = false;
                }
                chrono.setText(formatTime(pausedTime - baseTime));
                startButton.setText("Continue");
                resetButton.setVisibility(View.VISIBLE);
            } else {
                if (chronoStarted)
                    stopwatch.stop();
                stopwatch.baseTime = baseTime;
                stopwatch.start();
                chronoStarted = true;
                startButton.setText("Stop");
                resetButton.setVisibility(View.INVISIBLE);
            }
        }

        MyChrono.maximizeSize(chrono);

        SharedPreferences.Editor ed = options.edit();
        ed.putLong(PREFS_START_TIME, baseTime);
        ed.putLong(PREFS_PAUSED_TIME, pausedTime);
        ed.putBoolean(PREFS_ACTIVE, active);
        ed.putBoolean(PREFS_PAUSED, paused);
        ed.commit();
    } */

    void updateButtons() {
        if (!stopwatch.active) {
            startButton.setText("Start");
            resetButton.setVisibility(View.INVISIBLE);
        }
        else {
            if (stopwatch.paused) {
                startButton.setText("Continue");
                resetButton.setVisibility(View.VISIBLE);
            } else {
                startButton.setText("Stop");
                resetButton.setVisibility(View.INVISIBLE);
            }
        }
    }

    void pressReset() {
        stopwatch.reset();
        updateButtons();
    }

    void pressStart() {
        stopwatch.start();
        updateButtons();
    }

    public void onButtonStart(View v) {
        pressStart();
    }

    public void onButtonReset(View v) {
        pressReset();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN)
            return false;
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP /*|| keyCode == KeyEvent.KEYCODE_A*/) {
            pressStart();
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN /*|| keyCode == KeyEvent.KEYCODE_C*/) {
            pressReset();
            return true;
        }
        return false;
    }
}
