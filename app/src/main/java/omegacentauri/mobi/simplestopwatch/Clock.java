package omegacentauri.mobi.simplestopwatch;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Clock extends ShowTime {
    private Button secondButton;
    private Button firstButton;
    protected MyClock chrono;
    protected static final int TEXT_BUTTONS[] = {
    };
    protected static final int IMAGE_BUTTONS[][] = {
            {R.id.settings, R.drawable.settings},
            {R.id.menu, R.drawable.menu}
    };

    @Override
    public boolean noTouch() {
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        colorThemeOptionName = Options.PREF_CLOCK_COLOR;

        setContentView(R.layout.activity_clock);
        bigDigits = (BigTextView)findViewById(R.id.chrono);
        secondButton = (Button)findViewById(R.id.reset);
        firstButton = (Button)findViewById(R.id.start);
        controlBar = (LinearLayout)findViewById(R.id.controlBar);
        mainContainer = findViewById(R.id.main_view);
        textButtons = TEXT_BUTTONS;
        imageButtons = IMAGE_BUTTONS;

        setupChrono();
    }

    protected void setupChrono() {
        chrono = new MyClock(this, options, bigDigits, (TextView)findViewById(R.id.fraction),
                mainContainer);
        timeKeeper = chrono;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtons();
        if (Options.swipeEnabled(options) && !options.getBoolean(Options.PREF_CLOCK_SWIPE_INFO, false)) {
            SharedPreferences.Editor ed = options.edit();
            ed.putBoolean(Options.PREF_CLOCK_SWIPE_INFO, true);
            MyChrono.apply(ed);
            if (hasTouch())
                timedMessage("Clock Mode", "Swipe on time or press menu button to switch to stopwatch mode", 2500);
            else
                timedMessage("Clock Mode", "Press up/down or use menu button to switch to stopwatch mode", 2500);
//            Toast.makeText(this, "Clock mode: Swipe time to switch to stopwatch", Toast.LENGTH_LONG).show();
        }
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

        boolean fs = options.getString(Options.PREF_TAP_ACTION, "fullscreen").equals("fullscreen") && options.getBoolean(Options.PREF_FULLSCREEN, false);

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
            case R.id.clock:
                switchActivity(Clock.class, NONE);
                return true;
            case R.id.clock_with_seconds:
                switchActivity(ClockWithSeconds.class, NONE);
                return true;
            case R.id.fullscreen:
                toggleFullscreen();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
