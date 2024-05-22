package omegacentauri.mobi.simplestopwatch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class StopWatch extends ShowTime {
    private Button secondButton;
    private Button firstButton;
    private TextView laps;
    private MyChrono chrono;

    protected static final int TEXT_BUTTONS[] = {
            R.id.start, R.id.reset
    };
    protected static final int IMAGE_BUTTONS[][] = {
            {R.id.settings, R.drawable.settings},
            {R.id.menu, R.drawable.menu}
    };
    private boolean volumeControl;
    private View noTouchIcon;
    private boolean noTouch = false;
    private long lastNoTouchWarned = -1;
    private static final long NO_TOUCH_WARN_DELAY = 5 * 60 * 1000l;
    private View menuButton;
    private View settingsButton;

    @Override
    public boolean noTouch() {
        return noTouch && chrono.active && ! chrono.paused;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            noTouch = savedInstanceState.getBoolean("noTouch", false);

        colorThemeOptionName = Options.PREF_STOPWATCH_COLOR;

        String last = options.getString(Options.PREF_LAST_ACTIVITY, StopWatch.class.getName());
        if (! last.equals(StopWatch.class.getName())) {
            try {
                switchActivity(Class.forName(last), NONE);
            } catch (ClassNotFoundException e) {
                debug("class not found "+last);
            }
        }

        setContentView(R.layout.activity_stop_watch);
        bigDigits = (BigTextView)findViewById(R.id.chrono);
        secondButton = (Button)findViewById(R.id.reset);
        firstButton = (Button)findViewById(R.id.start);
        controlBar = (LinearLayout)findViewById(R.id.controlBar);
        mainContainer = findViewById(R.id.chrono_and_laps);
        noTouchIcon = findViewById(R.id.lock);
        menuButton = findViewById(R.id.menu);
        settingsButton = findViewById(R.id.settings);
        laps = (TextView)findViewById(R.id.laps);
        textButtons = TEXT_BUTTONS;
        imageButtons = IMAGE_BUTTONS;

        chrono = new MyChrono(this, options, bigDigits, (TextView)findViewById(R.id.fraction),
                laps, mainContainer);
        timeKeeper = chrono;

        laps.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                chrono.copyLapsToClipboard();
                return true;
            }
        });

        if (noTouch)
            lockModeWarn();
    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        b.putBoolean("noTouch", noTouch);
    }

    @Override
    protected void onResume() {
        super.onResume();

        volumeControl = options.getBoolean(Options.PREF_VOLUME, true);
        updateButtons();
        if (Options.swipeEnabled(options) && !options.getBoolean(Options.PREF_STOPWATCH_SWIPE_INFO, false)) {
            SharedPreferences.Editor ed = options.edit();
            ed.putBoolean(Options.PREF_STOPWATCH_SWIPE_INFO, true);
            MyChrono.apply(ed);
            if (hasTouch())
                timedMessage("StopWatch Mode", "Swipe on time or press menu button to switch to clock mode", 2500);
            else
                timedMessage("StopWatch Mode", "Press up/down or use menu button to switch to clock mode", 2500);
//            Toast.makeText(this, "StopWatch mode: Swipe time to switch to clock", Toast.LENGTH_LONG).show();
        }
    }

    void updateButtons() {
        if (!chrono.active) {
            firstButton.setText("Start");
            secondButton.setText("Delay");
        }
        else {
            if (chrono.paused) {
                firstButton.setText("Continue");
                secondButton.setText("Reset");
                //secondButton.setVisibility(View.VISIBLE);
            } else {
                firstButton.setText("Stop");
                secondButton.setText("Lap");
            }
        }

        if (noTouch()) {
//            secondButton.setVisibility(View.GONE);
            menuButton.setVisibility(View.GONE);
            settingsButton.setVisibility(View.GONE);
            noTouchIcon.setVisibility(View.VISIBLE);

            //this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        }
        else {
            secondButton.setVisibility(View.VISIBLE);
            menuButton.setVisibility(View.VISIBLE);
            settingsButton.setVisibility(
                    options.getBoolean(Options.PREF_SETTINGS_BUTTON, true) ? View.VISIBLE : View.GONE );
            noTouchIcon.setVisibility(View.GONE);

            //this.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION);
        }
    }

    @Override
    protected void setTheme() {
        super.setTheme();
        int fore = Options.getForeColor(this, options);

        laps.setTextColor(fore);
    }

    public void pace() {
        if (chrono.getTime() < 0 || !chrono.active) {
            Toast.makeText(StopWatch.this, "Stopwatch time not available", Toast.LENGTH_LONG).show();
            return;
        }

        lockOrientation();

        AlertDialog.Builder builder = AlertDialog_Builder();
        builder.setTitle("Pace/Speed Calculator");
        final long currentTime1000 = chrono.getTime();
        final String currentTimeString = chrono.formatTimeFull(currentTime1000);
        final double currentTime = currentTime1000/1000.;
        builder.setTitle("Pace Calculator");
        View content = getLayoutInflater().inflate(R.layout.pace, null);
        builder.setView(content);
        final EditText input = (EditText)content.findViewById(R.id.distance);
        final TextView message = (TextView)content.findViewById(R.id.message);
        final String defaultMessage = "Time: " + currentTimeString;
        final Button copyPace = (Button)content.findViewById(R.id.copy_pace);
        final Button copySpeed = (Button)content.findViewById(R.id.copy_speed);
        message.setText(defaultMessage);
        copyPace.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    double distance = Double.parseDouble(input.getEditableText().toString());
                    clip(StopWatch.this, chrono.formatTimeFull((long)(currentTime1000 / distance)));
                }
                catch(Exception e) {
                    Toast.makeText(StopWatch.this, "Units not validly set", Toast.LENGTH_LONG).show();
                }
            }
        });
        copySpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    double distance = Double.parseDouble(input.getEditableText().toString());
                    clip(StopWatch.this, String.format("%g", distance/(currentTime/(60*60))));
                }
                catch(Exception e) {
                    Toast.makeText(StopWatch.this, "Units not validly set", Toast.LENGTH_LONG).show();
                }
            }
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String msg;
                try {
                    double distance = Double.parseDouble(editable.toString());
                    msg = String.format("Time:\t%s\n" +
                                    "Pace:\t%s /unit\n" +
                                    "Speed:\t%g units/hour", currentTimeString,
                            chrono.formatTimeFull((long)(currentTime1000/distance)),
                            distance/(currentTime/(60*60)));
                }
                catch(Exception e) {
                    msg = defaultMessage;
                }
                SpannableStringBuilder span = new SpannableStringBuilder(msg);
                int w1 = (int) message.getPaint().measureText("Speed: ");
                //int w1 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 80, StopWatch.this.getResources().getDisplayMetrics());
                span.setSpan(new TabStopSpan.Standard(w1), 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
                message.setText(span);
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                setOrientation();
            }
        });

        input.requestFocus();
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    void pressSecondButton() {
        if (Build.VERSION.SDK_INT >= 5)
            secondButton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        chrono.secondButton();
        updateButtons();
    }

    @Override
    public void pressFirstButton() {
        if (Build.VERSION.SDK_INT >= 5)
            firstButton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        chrono.firstButton();
        updateButtons();
    }

    public void onButtonStart(View v) {
        pressFirstButton();
    }

    public void onButtonReset(View v) {
        pressSecondButton();
    }

    public boolean isFirstButton(int keyCode) {
        return (keyCode == KeyEvent.KEYCODE_VOLUME_UP && (volumeControl || noTouch()) ) ||
//                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
    }

    public boolean isSecondButton(int keyCode) {
        return (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && (volumeControl || noTouch() ) ) ||
                keyCode == KeyEvent.KEYCODE_MEDIA_REWIND;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (volumeControl && (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) )
            return true;
        else
            return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (noTouch())
            touchWarn();
        else
            super.onBackPressed();
    }

    public void touchWarn() {
        if (lastNoTouchWarned < 0 || lastNoTouchWarned + NO_TOUCH_WARN_DELAY <= System.currentTimeMillis()) {
            lastNoTouchWarned = System.currentTimeMillis();
            Toast.makeText(this, "Screen locked: Press Volume Up to stop", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent v) {
        if (noTouch()) {
            touchWarn();
            return true;
        }
        else
            return super.dispatchTouchEvent(v);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            if (volumeControl && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))
                return true;
            else
                return super.onKeyDown(keyCode, event);
        }
        if (isFirstButton(keyCode)) {
            pressFirstButton();
            return true;
        }
        else if (isSecondButton(keyCode)) {
            pressSecondButton();
            return true;
        }
//        else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
//            onButtonSettings(null);
//        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.copy_laps).setVisible(chrono.lapData.length()>0);
        menu.findItem(R.id.clear_laps).setVisible(chrono.lapData.length()>0);
        menu.findItem(R.id.pace).setVisible(chrono.getTime()>0);
        menu.findItem(R.id.lock_mode).setVisible(!noTouch);
        menu.findItem(R.id.unlock_mode).setVisible(noTouch);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        debug("options menu "+item.getItemId());
        switch (item.getItemId()) {
            case R.id.copy_time:
                chrono.copyToClipboard();
                return true;
            case R.id.copy_laps:
                chrono.copyLapsToClipboard();
                return true;
            case R.id.clear_laps:
                chrono.clearLapData();
                return true;
            case R.id.pace:
                pace();
                return true;
            case R.id.clock:
                switchActivity(Clock.class, NONE);
                return true;
            case R.id.clock_with_seconds:
                switchActivity(ClockWithSeconds.class, NONE);
                return true;

            case R.id.lock_mode:
                noTouch = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    invalidateOptionsMenu();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && options.getBoolean(Options.PREF_PIN_ON_LOCK, true)) {
                    startLockTask();
                }
                updateButtons();
                lockModeWarn();
                return true;
            case R.id.unlock_mode:
                noTouch = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    invalidateOptionsMenu();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && options.getBoolean(Options.PREF_PIN_ON_LOCK, true)) {
                    stopLockTask();
                }
                updateButtons();
                return true;
            case R.id.fullscreen:
                toggleFullscreen();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void lockModeWarn() {
        Toast.makeText(this, "Touch screen will be disabled when stopwatch is running. Use Volume Up to stop and Volume Down for lap.", Toast.LENGTH_LONG).show();
    }

}
