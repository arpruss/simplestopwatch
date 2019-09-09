package omegacentauri.mobi.simplestopwatch;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.TabStopSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class StopWatch extends Activity {
    private static final boolean DEBUG = true;
    private static final int MENU_COPY_TIME = 0;
    private static final int MENU_COPY_LAP_DATA = 1;
    private static final int MENU_CLEAR_LAP_DATA = 2;
    private static final int MENU_PACE = 3;
    SharedPreferences options;
    long baseTime = 0;
    long pausedTime = 0;
    boolean active = false;
    boolean paused = false;
    boolean chronoStarted = false;
    private BigTextView chrono = null;
    private MyChrono stopwatch;
    private Button secondButton;
    private Button firstButton;
    private float unselectedThickness = 2f;
    private float selectedThickness = 6f;
    private static final int RECOLORABLE_TEXTVIEW[] = {
        R.id.fraction, R.id.laps
    };
    private static final int RECOLORABLE_BUTTON[] = {
            R.id.start, R.id.reset
    };
    private View.OnTouchListener highlighter;
    private TextView laps;

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
        secondButton = (Button)findViewById(R.id.reset);
        firstButton = (Button)findViewById(R.id.start);
        laps = (TextView)findViewById(R.id.laps);
        stopwatch = new MyChrono(this, options, chrono, (TextView)findViewById(R.id.fraction),
                laps);
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
/*        secondButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (stopwatch.active && stopwatch.lapData.length() > 0) {
                    askClearLapData();
                }
                return true;
            }
        }); */
        firstButton.setOnTouchListener(highlighter);
        secondButton.setOnTouchListener(highlighter);
        chrono.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                stopwatch.copyToClipboard();
                return false;
            }
        });
        laps.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                stopwatch.copyLapsToClipboard();
                return false;
            }
        });
    }

    private void askClearLapData() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Clear lap data");
        alertDialog.setMessage("Clear lap data?");
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        stopwatch.clearLapData();
                    } });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {} });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {} });
        alertDialog.show();
    }

    void setTheme() {
        chrono.setFont(Options.getFont(options));
        chrono.setKeepAspect(options.getBoolean(Options.PREF_KEEP_ASPECT, true));
        chrono.setLineSpacing(Float.parseFloat(options.getString(Options.PREF_LINE_SPACING, "105%").replace("%",""))/100f);
        chrono.setLetterSpacing(Float.parseFloat(options.getString(Options.PREF_LETTER_SPACING, "95%").replace("%",""))/100f);
        chrono.setScale(Float.parseFloat(options.getString(Options.PREF_SCALE, "98%").replace("%",""))/100f);

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
        ((ImageButton)findViewById(R.id.menu)).setColorFilter(fore, PorterDuff.Mode.MULTIPLY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setVolumeControlStream(Options.getStream(options));

        String o = options.getString(Options.PREFS_ORIENTATION, "automatic");
        if (o.equals("landscape"))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else if (o.equals("portrait"))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        debug("theme");
        setTheme();
        int orientation = getResources().getConfiguration().orientation;
        chrono.post(new Runnable() {
            @Override
            public void run() {
                stopwatch.updateViews();
            }
        });

        debug("onResume");

        stopwatch.restore();
        stopwatch.updateViews();
        updateButtons();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        debug("onConfChanged");
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
            firstButton.setText("Start");
            secondButton.setText("Delay");
        }
        else {
            if (stopwatch.paused) {
                firstButton.setText("Continue");
                secondButton.setText("Reset");
                //secondButton.setVisibility(View.VISIBLE);
            } else {
                firstButton.setText("Stop");
                secondButton.setText("Lap");
            }
        }
    }

    void pressReset() {
        stopwatch.secondButton();
        updateButtons();
    }

    void pressStart() {
        stopwatch.firstButton();
        updateButtons();
    }

    public void onButtonStart(View v) {
        pressStart();
    }

    public void onButtonReset(View v) {
        pressReset();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!options.getBoolean(Options.PREF_VOLUME, true))
            return false;
        return keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!options.getBoolean(Options.PREF_VOLUME, true))
            return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN)
            return keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN;
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            pressStart();
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            pressReset();
            return true;
        }
        return false;
    }

    public void onButtonSettings(View view) {
        startActivity(new Intent(this, Options.class));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopwatch.destroy();
    }

    public static void debug(String s) {
        if (DEBUG)
            Log.v("chrono", s);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        debug("options menu "+item.getItemId());
        switch (item.getItemId()) {
            case MENU_COPY_TIME:
                stopwatch.copyToClipboard();
                return true;
            case MENU_COPY_LAP_DATA:
                stopwatch.copyLapsToClipboard();
                return true;
            case MENU_CLEAR_LAP_DATA:
                stopwatch.clearLapData();
                return true;
            case MENU_PACE:
                pace();
                return true;
        }
        return false;
    }

    public void pace() {
        if (stopwatch.getTime() < 0 || !stopwatch.active) {
            Toast.makeText(StopWatch.this, "Stopwatch time not available", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pace/Speed Calculator");
        final long currentTime1000 = stopwatch.getTime();
        final String currentTimeString = stopwatch.formatTimeFull(currentTime1000);
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            copyPace.setOnClickListener(new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                @Override
                public void onClick(View view) {
                    try {
                        double distance = Double.parseDouble(input.getEditableText().toString());
                        ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData data = ClipData.newPlainText("pace", stopwatch.formatTimeFull((long)(currentTime1000 / distance)));
                        clip.setPrimaryClip(data);
                    }
                    catch(Exception e) {
                        Toast.makeText(StopWatch.this, "Units not validly set", Toast.LENGTH_LONG).show();
                    }
                }
            });
            copySpeed.setOnClickListener(new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                @Override
                public void onClick(View view) {
                    try {
                        double distance = Double.parseDouble(input.getEditableText().toString());
                        ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData data = ClipData.newPlainText("speed", String.format("%g", distance/(currentTime/(60*60))));
                        clip.setPrimaryClip(data);
                    }
                    catch(Exception e) {
                        Toast.makeText(StopWatch.this, "Units not validly set", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        else {
            copySpeed.setVisibility(View.GONE);
            copyPace.setVisibility(View.GONE);
        }

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
                                        stopwatch.formatTimeFull((long)(currentTime1000/distance)),
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

        input.requestFocus();
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
        debug("pace");
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.copy_laps).setVisible(stopwatch.lapData.length()>0);
        menu.findItem(R.id.clear_laps).setVisible(stopwatch.lapData.length()>0);

/*        menu.clear();
        menu.add(0, MENU_COPY_TIME, 00, "Copy time to clipboard");
        if (stopwatch.lapData.length()>0) {
            menu.add(0, MENU_COPY_LAP_DATA, 0, "Copy laps to clipboard");
            menu.add(0, MENU_CLEAR_LAP_DATA, 0, "Clear lap data");
        }
        menu.add(0, MENU_PACE, 0, "Pace and speed"); */
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public void onButtonMenu(View view) {
        openOptionsMenu();
    }
}
