package omegacentauri.mobi.simplestopwatch;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.TabStopSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class StopWatch extends Activity {
    private static final boolean DEBUG = true;
    SharedPreferences options;
    private BigTextView chrono = null;
    private MyChrono stopwatch;
    private Button secondButton;
    private Button firstButton;
    private float unselectedThickness = 2f;
    private float selectedThickness = 6f;
    private static final int RECOLORABLE_BUTTON[] = {
            R.id.start, R.id.reset
    };
    private View.OnTouchListener highlighter;
    private TextView laps;
    private View.OnTouchListener imageHighlighter;
    private LinearLayout controlBar;

    public float dp2px(float dp){
        return dp * (float)getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT;
    }

    public int getControlBarForeColor() {
        int base = Options.getForeColor(options);
        if (!options.getBoolean(Options.PREF_FULLSCREEN, false))
            return base;
        int controlBarBrightness = 255 * Integer.parseInt(options.getString(Options.PREF_FS_BRIGHT, "0")) / 100;
        if (controlBarBrightness == 0)
            return base;
        return (base & 0xFFFFFF) | (controlBarBrightness << 24);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        options = PreferenceManager.getDefaultSharedPreferences(this);
        MyChrono.detectBoot(options);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_stop_watch);
        chrono = (BigTextView)findViewById(R.id.chrono);
        secondButton = (Button)findViewById(R.id.reset);
        firstButton = (Button)findViewById(R.id.start);
        controlBar = (LinearLayout)findViewById(R.id.controlBar);
        laps = (TextView)findViewById(R.id.laps);
        stopwatch = new MyChrono(this, options, chrono, (TextView)findViewById(R.id.fraction),
                laps);
        highlighter = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    GradientDrawable gd = (GradientDrawable) view.getBackground();
                    gd.setStroke((int)dp2px(selectedThickness), getControlBarForeColor());
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    GradientDrawable gd = (GradientDrawable) view.getBackground();
                    gd.setStroke((int)dp2px(unselectedThickness), getControlBarForeColor());
                }
                return false;
            }
        };
        firstButton.setOnTouchListener(highlighter);
        secondButton.setOnTouchListener(highlighter);
        /*
        imageHighlighter = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    ((ImageButton)view).setColorFilter(Options.getHighlightColor(options), PorterDuff.Mode.MULTIPLY);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    ((ImageButton)view).setColorFilter(Options.getForeColor(options), PorterDuff.Mode.MULTIPLY);
                }
                return false;
            }
        };

        ((ImageButton)findViewById(R.id.settings)).setOnTouchListener(imageHighlighter);
        ((ImageButton)findViewById(R.id.menu)).setOnTouchListener(imageHighlighter);
        */

        chrono.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                stopwatch.copyToClipboard();
                return false;
            }
        });
        chrono.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!options.getBoolean(Options.PREF_CONTROL_FULLSCREEN, true))
                    return;
                SharedPreferences.Editor ed = options.edit();
                ed.putBoolean(Options.PREF_FULLSCREEN, ! options.getBoolean(Options.PREF_FULLSCREEN, false));
                MyChrono.apply(ed);
                setFullScreen();
                setTheme();
            }
        });
        laps.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                stopwatch.copyLapsToClipboard();
                return false;
            }
        });
        setFullScreen();
        setTheme();
    }

    void setTheme() {
        chrono.setFont(Options.getFont(options));
        chrono.setKeepAspect(options.getBoolean(Options.PREF_KEEP_ASPECT, true));
        chrono.setLineSpacing(Float.parseFloat(options.getString(Options.PREF_LINE_SPACING, "105%").replace("%",""))/100f);
        chrono.setLetterSpacing(Float.parseFloat(options.getString(Options.PREF_LETTER_SPACING, "95%").replace("%",""))/100f);
        chrono.setScale(Float.parseFloat(options.getString(Options.PREF_SCALE, "98%").replace("%",""))/100f);

        int fore = Options.getForeColor(options);
        int controlFore = getControlBarForeColor();
        int back = Options.getBackColor(options);

        ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0).setBackgroundColor(back);

        laps.setTextColor(fore);
        ((TextView)findViewById(R.id.fraction)).setTextColor(controlFore);
        debug(String.format("controlFore=%x", controlFore));

        chrono.setTextColor(fore);

        for (int id : RECOLORABLE_BUTTON) {
            Button b = findViewById(id);
            b.setTextColor(controlFore);
            GradientDrawable gd = (GradientDrawable)b.getBackground();
            gd.setStroke((int)dp2px(unselectedThickness), controlFore);
        }

        ((ImageButton)findViewById(R.id.settings)).setColorFilter(controlFore, PorterDuff.Mode.MULTIPLY);
        ((ImageButton)findViewById(R.id.menu)).setColorFilter(controlFore, PorterDuff.Mode.MULTIPLY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(back);
            View dv = window.getDecorView();
            int flags = dv.getSystemUiVisibility();
            if (back == Color.WHITE)
                dv.setSystemUiVisibility(flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR /* | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR */);
            else
                dv.setSystemUiVisibility(flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR /* & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR */);
//            window.setNavigationBarColor(back);
        }
    }

    void setOrientation() {
        String o = options.getString(Options.PREF_ORIENTATION, "automatic");
        if (o.equals("landscape"))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else if (o.equals("portrait"))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

    }

    private void setFullScreen()
    {
        if (!options.getBoolean(Options.PREF_CONTROL_FULLSCREEN, true))
            return;

        boolean fs = options.getBoolean(Options.PREF_FULLSCREEN, false);
        int controlBarBrightness = 0;
        if (fs) {
            controlBarBrightness = 255 * Integer.parseInt(options.getString(Options.PREF_FS_BRIGHT, "0")) / 100;
        }
        Window w = getWindow();
        WindowManager.LayoutParams attrs = w.getAttributes();

        if (options.getBoolean(Options.PREF_FULLSCREEN, false)) {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        else {
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }

        w.setAttributes(attrs);

        View dv = w.getDecorView();

        if(Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            dv.setSystemUiVisibility(fs ? View.GONE : View.VISIBLE);
        } else if(Build.VERSION.SDK_INT >= 19) {
            int flags = dv.getSystemUiVisibility();
            if (fs)
                flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            else
                flags &= ~(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            dv.setSystemUiVisibility(flags);
        }

        controlBar.setVisibility((fs && controlBarBrightness == 0) ? View.GONE : View.VISIBLE);
        if (controlBarBrightness > 0 && fs)
            controlBar.setBackgroundColor(Options.getBackColor(options) & (controlBarBrightness<<24));
        else
            controlBar.setBackgroundColor(Options.getBackColor(options) | 0xFF000000);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        if (! fs) {
            lp.addRule(RelativeLayout.ABOVE, R.id.controlBar);
        }
        else {
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }
        laps.setLayoutParams(lp);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setVolumeControlStream(Options.getStream(options));

        setOrientation();

        setTheme();
        setFullScreen();
        debug("theme");
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
            case R.id.copy_time:
                stopwatch.copyToClipboard();
                return true;
            case R.id.copy_laps:
                stopwatch.copyLapsToClipboard();
                return true;
            case R.id.clear_laps:
                stopwatch.clearLapData();
                return true;
            case R.id.pace:
                pace();
                return true;
        }
        return false;
    }

    public void lockOrientation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            switch(((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation()) {
                case Surface.ROTATION_0:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
                case Surface.ROTATION_90:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;
                case Surface.ROTATION_180:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    break;
                case Surface.ROTATION_270:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    break;
            }
        }
    }

    public void pace() {
        if (stopwatch.getTime() < 0 || !stopwatch.active) {
            Toast.makeText(StopWatch.this, "Stopwatch time not available", Toast.LENGTH_LONG).show();
            return;
        }

        lockOrientation();

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
        copyPace.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    double distance = Double.parseDouble(input.getEditableText().toString());
                    clip(StopWatch.this, stopwatch.formatTimeFull((long)(currentTime1000 / distance)));
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
        debug("pace");
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.copy_laps).setVisible(stopwatch.lapData.length()>0);
        menu.findItem(R.id.clear_laps).setVisible(stopwatch.lapData.length()>0);

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

    public static void clip(Context c, String s) {
        android.text.ClipboardManager clip = (android.text.ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE);
        clip.setText(s);
        Toast.makeText(c, "Copied", Toast.LENGTH_SHORT).show();
    }
}
