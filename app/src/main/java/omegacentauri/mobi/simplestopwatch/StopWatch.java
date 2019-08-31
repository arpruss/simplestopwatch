package omegacentauri.mobi.simplestopwatch;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;

public class StopWatch extends Activity {
    SharedPreferences options;
    long baseTime = 0;
    long pausedTime = 0;
    boolean active = false;
    boolean paused = false;
    boolean chronoStarted = false;
    private TextView chrono1 = null;
    private TextView chrono2 = null;
    private MyChrono stopwatch;
    private Button resetButton;
    private Button startButton;
    private static final int RECOLORABLE_TEXTVIEW[] = {
        R.id.chrono1, R.id.chrono2, R.id.fraction
    };
    private static final int RECOLORABLE_BUTTON[] = {
            R.id.start, R.id.reset
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        options = PreferenceManager.getDefaultSharedPreferences(this);
        MyChrono.detectBoot(options);
        setContentView(R.layout.activity_stop_watch);
        chrono1 = (TextView)findViewById(R.id.chrono1);
        chrono2 = (TextView)findViewById(R.id.chrono2);
        resetButton = (Button)findViewById(R.id.reset);
        startButton = (Button)findViewById(R.id.start);
        stopwatch = new MyChrono(this, options, chrono1, chrono2, (TextView)findViewById(R.id.fraction));
    }

    void setColorScheme() {
        int fore = Options.getForeColor(options);
        int back = Options.getBackColor(options);

        ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0).setBackgroundColor(back);

        for (int id : RECOLORABLE_TEXTVIEW) {
            ((TextView)findViewById(id)).setTextColor(fore);
           // ((TextView)findViewById(id)).setBackgroundColor(back);

        }
        int buttonBack = fore == Color.WHITE ? 0xFFD0D0D0 :
                         fore == Color.BLACK ? 0xFF303030 :
                                 fore;
        for (int id : RECOLORABLE_BUTTON) {
            Button b = findViewById(id);
            b.setTextColor(back);
            b.setBackgroundColor(buttonBack);
        }
        ((ImageButton)findViewById(R.id.settings)).setColorFilter(fore, PorterDuff.Mode.MULTIPLY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setColorScheme();
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.v("chrono", "landscape");
            chrono2.setVisibility(View.GONE);
        } else {
            Log.v("chrono", "portrait");
            chrono2.setVisibility(View.VISIBLE);
        }
        chrono1.post(new Runnable() {
            @Override
            public void run() {
                stopwatch.updateViews();
            }
        });

        Log.v("chrono", "onResume");

        stopwatch.restore();
        stopwatch.updateViews();
        updateButtons();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v("chrono", "onConfChanged");
        super.onConfigurationChanged(newConfig);
        stopwatch.updateViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopwatch.save();
        stopwatch.stopUpdating();
    }

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
        stopwatch.resetButton();
        updateButtons();
    }

    void pressStart() {
        stopwatch.startStopButton();
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

    public void onButtonSettings(View view) {
        startActivity(new Intent(this, Options.class));
    }
}
