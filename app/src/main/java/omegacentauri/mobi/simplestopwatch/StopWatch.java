package omegacentauri.mobi.simplestopwatch;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class StopWatch extends Activity {
    SharedPreferences options;
    long baseTime = 0;
    long pausedTime = 0;
    boolean active = false;
    boolean paused = false;
    boolean chronoStarted = false;
    private BigTextView chrono = null;
    private MyChrono stopwatch;
    private Button resetButton;
    private Button startButton;
    private float unselectedThickness = 2f;
    private float selectedThickness = 6f;
    private static final int RECOLORABLE_TEXTVIEW[] = {
        R.id.fraction
    };
    private static final int RECOLORABLE_BUTTON[] = {
            R.id.start, R.id.reset
    };
    private View.OnTouchListener highlighter;

    public float dp2px(float dp){
        return dp * (float)getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        options = PreferenceManager.getDefaultSharedPreferences(this);
        MyChrono.detectBoot(options);
        setContentView(R.layout.activity_stop_watch);
        chrono = (BigTextView)findViewById(R.id.chrono);
        resetButton = (Button)findViewById(R.id.reset);
        startButton = (Button)findViewById(R.id.start);
        stopwatch = new MyChrono(this, options, chrono, (TextView)findViewById(R.id.fraction));
        highlighter = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    GradientDrawable gd = (GradientDrawable) view.getBackground();
                    gd.setStroke((int)dp2px(selectedThickness), Options.getForeColor(options));
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    GradientDrawable gd = (GradientDrawable) view.getBackground();
                    gd.setStroke((int)dp2px(unselectedThickness), Options.getForeColor(options));
                }
                return false;
            }
        };
        startButton.setOnTouchListener(highlighter);
        resetButton.setOnTouchListener(highlighter);
    }

    void setTheme() {
        chrono.setFont(Options.getFont(options));

        int fore = Options.getForeColor(options);
        int back = Options.getBackColor(options);

        ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0).setBackgroundColor(back);

        for (int id : RECOLORABLE_TEXTVIEW) {
            ((TextView)findViewById(id)).setTextColor(fore);
        }

        chrono.setTextColor(fore);

        for (int id : RECOLORABLE_BUTTON) {
            Button b = findViewById(id);
            b.setTextColor(fore);
            GradientDrawable gd = (GradientDrawable)b.getBackground();
            gd.setStroke((int)dp2px(unselectedThickness), fore);
        }

        ((ImageButton)findViewById(R.id.settings)).setColorFilter(fore, PorterDuff.Mode.MULTIPLY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String o = options.getString(Options.PREFS_ORIENTATION, "automatic");
        if (o.equals("landscape"))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else if (o.equals("portrait"))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        Log.v("chrono", "theme");
        setTheme();
        int orientation = getResources().getConfiguration().orientation;
        chrono.post(new Runnable() {
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
        //stopwatch.save();
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
