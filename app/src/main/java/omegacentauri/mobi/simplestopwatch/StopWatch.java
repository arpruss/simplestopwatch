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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        laps = (TextView)findViewById(R.id.laps);
        textButtons = TEXT_BUTTONS;
        imageButtons = IMAGE_BUTTONS;

        chrono = new MyChrono(this, options, bigDigits, (TextView)findViewById(R.id.fraction),
                laps, mainContainer);
        timeKeeper = chrono;

        bigDigits.setOnTouchListener(gestureListener);
        laps.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                chrono.copyLapsToClipboard();
                return true;
            }
        });

        if (!options.getBoolean(Options.PREF_STOPWATCH_SWIPE_INFO, false)) {
            SharedPreferences.Editor ed = options.edit();
            ed.putBoolean(Options.PREF_STOPWATCH_SWIPE_INFO, true);
            MyChrono.apply(ed);
            Toast.makeText(this, "StopWatch mode: Swipe time to switch to clock", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        volumeControl = options.getBoolean(Options.PREF_VOLUME, true);
        updateButtons();
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
    }

    @Override
    protected void setTheme() {
        super.setTheme();
        int fore = Options.getForeColor(options);

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

    void pressFirstButton() {
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
        return (keyCode == KeyEvent.KEYCODE_VOLUME_UP && volumeControl) ||
//                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
    }

    public boolean isSecondButton(int keyCode) {
        return (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && volumeControl) ||
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void flingLeft() {
        switchActivity(Clock.class, LEFT);
    }

    @Override
    protected void flingRight() {
        switchActivity(Clock.class, RIGHT);
    }

    @Override
    protected void flingUp() {
        switchActivity(Clock.class, UP);
    }

    @Override
    protected void flingDown() {
        switchActivity(Clock.class, DOWN);
    }
}
