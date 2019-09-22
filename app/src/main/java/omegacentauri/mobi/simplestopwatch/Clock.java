package omegacentauri.mobi.simplestopwatch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.TabStopSpan;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Clock extends ShowTime {
    private Button secondButton;
    private Button firstButton;
    private MyClock chrono;
    protected static final int TEXT_BUTTONS[] = {
    };
    protected static final int IMAGE_BUTTONS[][] = {
            {R.id.settings, R.drawable.settings},
            {R.id.menu, R.drawable.menu}
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_clock);
        bigDigits = (BigTextView)findViewById(R.id.chrono);
        secondButton = (Button)findViewById(R.id.reset);
        firstButton = (Button)findViewById(R.id.start);
        controlBar = (LinearLayout)findViewById(R.id.controlBar);
        mainContainer = findViewById(R.id.main_view);
        textButtons = TEXT_BUTTONS;
        imageButtons = IMAGE_BUTTONS;

        chrono = new MyClock(this, options, bigDigits, (TextView)findViewById(R.id.fraction),
                mainContainer);
        timeKeeper = chrono;

        bigDigits.setOnTouchListener(gestureListener);

        if (!options.getBoolean(Options.PREF_CLOCK_SWIPE_INFO, false)) {
            SharedPreferences.Editor ed = options.edit();
            ed.putBoolean(Options.PREF_CLOCK_SWIPE_INFO, true);
            MyChrono.apply(ed);
            timedMessage("Clock Mode", "Swipe on time or press menu button to switch to stopwatch mode", 5500);
//            Toast.makeText(this, "Clock mode: Swipe time to switch to stopwatch", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtons();
    }

    void updateButtons() {
    }

    @Override
    protected void setTheme() {
        super.setTheme();
    }

    @Override
    protected void setFullScreen() {
        super.setFullScreen();

        boolean fs = options.getBoolean(Options.PREF_CONTROL_FULLSCREEN, true) && options.getBoolean(Options.PREF_FULLSCREEN, false);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        if (! fs) {
            lp.addRule(RelativeLayout.ABOVE, R.id.controlBar);
        }
        else {
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }
        mainContainer.setLayoutParams(lp);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.onKeyDown(keyCode, event);
        }
        else if (keyCode == KeyEvent.KEYCODE_MENU) {
            onButtonMenu(null);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.clockmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        debug("options menu "+item.getItemId());
        switch (item.getItemId()) {
            case R.id.copy_time:
                chrono.copyToClipboard();
                return true;
            case R.id.stopwatch:
                switchActivity(StopWatch.class, NONE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void flingLeft() {
        switchActivity(StopWatch.class, LEFT);
    }

    @Override
    protected void flingRight() {
        switchActivity(StopWatch.class, RIGHT);
    }

    @Override
    protected void flingUp() {
        switchActivity(StopWatch.class, UP);
    }

    @Override
    protected void flingDown() {
        switchActivity(StopWatch.class, DOWN);
    }

}
