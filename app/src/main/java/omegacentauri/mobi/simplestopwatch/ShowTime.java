package omegacentauri.mobi.simplestopwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
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
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.security.Key;

abstract public class ShowTime extends Activity {
    private static final boolean DEBUG = false;
    private static final int DARK_THEME = Build.VERSION.SDK_INT >= 23 ?
            android.R.style.Theme_DeviceDefault_Dialog_Alert :
            Build.VERSION.SDK_INT >= 14 ?
                    AlertDialog.THEME_DEVICE_DEFAULT_DARK :
                    4;
    SharedPreferences options;
    private static final float swipeAxisRatio = 1.33f;
    private static final float minimumFling = 0.5f;
    private static final float minimumVelocity = 0.2f;
    private static final float unselectedThickness = 2f;
    private static final float focusedThickness = 6f;
    private static final float selectedThickness = 9f;
    protected View mainContainer;
    protected static int textButtons[] = {};
    protected static int imageButtons[][] = {};
    protected View controlBar;
    protected static final int NONE = 0;
    protected static final int LEFT = 1;
    protected static final int RIGHT = 2;
    protected static final int DOWN = 3;
    protected static final int UP = 4;
    String colorThemeOptionName = Options.PREF_STOPWATCH_COLOR;
    //static final Class activityCircle[] = { StopWatch.class, Clock.class, ClockWithSeconds.class };

//    protected View.OnClickListener fullScreenListener;
    public BigTextView bigDigits;
    protected MyTimeKeeper timeKeeper;
    private GestureDetector gestureDetector;
    private View.OnTouchListener gestureListener;
    protected String controlScheme;

    Class[] getActivityCircle() {
        Class activityCircle[];
        if (options.getBoolean(Options.PREF_CLOCK_LITTLE_SECONDS, true)) {
            if (options.getBoolean(Options.PREF_CLOCK_BIG_SECONDS, true)) {
                activityCircle = new Class[] { StopWatch.class, Clock.class, ClockWithSeconds.class };
            }
            else {
                activityCircle = new Class[] { StopWatch.class, Clock.class };
            }
        }
        else {
            if (options.getBoolean(Options.PREF_CLOCK_BIG_SECONDS, true)) {
                activityCircle = new Class[] { StopWatch.class, ClockWithSeconds.class };
            }
            else {
                activityCircle = new Class[] { StopWatch.class };
            }
        }
        return activityCircle;
    }

    Class nextActivity(int delta) {
        Class[] activityCircle = getActivityCircle();
        for (int i=0; i< activityCircle.length; i++) {
            if (this.getClass() == activityCircle[i]) {
                int j = i + delta;
                if (j < 0) {
                    j = activityCircle.length - 1;
                }
                else {
                    j %= activityCircle.length;
                }
                debug(""+activityCircle[j]);
                return activityCircle[j];
            }
        }
        return activityCircle[0];
    }

    public float dp2px(float dp){
        return dp * (float)getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT;
    }

    abstract public boolean noTouch();

    public int getControlBarForeColor() {
        int base = Options.getForeColor(this, options);
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

        PreferenceManager.setDefaultValues(this, R.xml.options, true);

        options = PreferenceManager.getDefaultSharedPreferences(this);
        MyChrono.detectBoot(options);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        gestureListener = new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        };
    }

    static int[] getStateDescription(int focused, int pressed) {
        if (focused==0 && pressed==0)
            return new int[]{};
        else if (focused>0 && pressed==0)
            return new int[]{android.R.attr.state_focused};
        else if (focused==0 && pressed>0)
            return new int[]{android.R.attr.state_pressed};
        else //if (focused>0 && pressed>0)
            return new int[]{android.R.attr.state_pressed, android.R.attr.state_focused};
    }

    int focusedColor(int baseColor) {
        return 0x80000000 | (baseColor & 0x00FFFFFF);
    }

    StateListDrawable makeOvalButtonBackground(int controlFore) {
        StateListDrawable d = new StateListDrawable();
        for (int focused = 1 ; focused >= 0; focused--)
            for(int pressed = 1; pressed >= 0; pressed--) {
                GradientDrawable gd = (GradientDrawable) getResources().getDrawable(R.drawable.oval); //.getConstantState().newDrawable();
                gd.setStroke((int)dp2px(
                        pressed>0 ? selectedThickness :
                        focused>0 ? focusedThickness :
                        unselectedThickness
                        ), focused>0 ? focusedColor(controlFore) : controlFore);
                d.addState(getStateDescription(focused, pressed), gd);
            }

        return d;
    }

    void setTheme() {
        bigDigits.setFont(Options.getFont(options));
        bigDigits.setMaxAspect(Options.getMaxAspect(options));
        bigDigits.setLineSpacing(Float.parseFloat(options.getString(Options.PREF_LINE_SPACING, "105%").replace("%",""))/100f);
        bigDigits.setLetterSpacing(Float.parseFloat(options.getString(Options.PREF_LETTER_SPACING, "95%").replace("%",""))/100f);
        bigDigits.setScale(Float.parseFloat(options.getString(Options.PREF_SCALE, "98%").replace("%",""))/100f);

        int fore = Options.getForeColor(this, options);
        int controlFore = getControlBarForeColor();
        int back = Options.getBackColor(this, options);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(back);
        }

        ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0).setBackgroundColor(back);

        ((TextView)findViewById(R.id.fraction)).setTextColor(controlFore);
        debug(String.format("controlFore=%x", controlFore));

        bigDigits.setTextColor(fore);
        bigDigits.setDimFraction(Double.parseDouble(options.getString(Options.PREF_ONPAUSE,"70%").replace("%",""))/100.);

        for (int id : textButtons) {
            Button b = findViewById(id);
            if (b != null) {
                b.setTextColor(controlFore);
                b.setBackgroundDrawable(makeOvalButtonBackground(controlFore));
            }
        }

        for (int[] ids : imageButtons) {
            ImageButton b = findViewById(ids[0]);
            if (b != null) {
                b.setImageDrawable(new MyStateDrawable(this, ids[1]));
                b.setColorFilter(controlFore, PorterDuff.Mode.MULTIPLY);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(back);
            View dv = window.getDecorView();
            int flags = dv.getSystemUiVisibility();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (back == Color.WHITE)
                    dv.setSystemUiVisibility(flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR /* | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR */);
                else
                    dv.setSystemUiVisibility(flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR /* & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR */);
            }
        }
    }

    void setOrientation() {
        if (isTV())
            return;
        String o = options.getString(Options.PREF_ORIENTATION, "automatic");
        try {
            if (o.equals("landscape")) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            else if (o.equals("portrait"))
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            else if (o.equals("system")) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            }
            else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    }
                    else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    }
            }
        } catch(Exception e) {
            try {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }
            catch(Exception e2) {}
        }
    }

    public void toggleFullscreen() {
        SharedPreferences.Editor ed = options.edit();
        ed.putBoolean(Options.PREF_FULLSCREEN, ! options.getBoolean(Options.PREF_FULLSCREEN, false));
        MyChrono.apply(ed);
        setFullScreen();
        setTheme();
    }

    protected void setFullScreen()
    {
        boolean fs = options.getBoolean(Options.PREF_FULLSCREEN, false);
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
        int controlBarBrightness = 0;
        if (fs) {
            controlBarBrightness = 255 * Integer.parseInt(options.getString(Options.PREF_FS_BRIGHT, "0")) / 100;
        }

        controlBar.setVisibility((fs && controlBarBrightness == 0) ? View.GONE : View.VISIBLE);
        if (controlBarBrightness > 0 && fs)
            controlBar.setBackgroundColor(Options.getBackColor(this, options) & (controlBarBrightness<<24));
        else
            controlBar.setBackgroundColor(Options.getBackColor(this, options) | 0xFF000000);

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
    protected void onResume() {
        super.onResume();

        findViewById(R.id.settings).setVisibility( options.getBoolean(Options.PREF_SETTINGS_BUTTON, true) ? View.VISIBLE : View.GONE );

        controlScheme = options.getString(Options.PREF_SCHEME, Options.PREF_SCHEME_START_STOP);
        /*
        Class a = nextActivity(0);
        if (a != this.getClass()) {
            switchActivity(a, NONE);
            return;
        }
         */
        final DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                String tapAction =options.getString(Options.PREF_TAP_ACTION, "fullscreen");
                if (tapAction.equals("start_stop")) {
                    pressFirstButton();
                }
                else if (tapAction.equals("button2")) {
                    pressSecondButton();
                }
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                debug("singleTapUp");
                if (!options.getString(Options.PREF_TAP_ACTION, "fullscreen").equals("fullscreen"))
                    return false;
                toggleFullscreen();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {
                String tapAction = options.getString(Options.PREF_TAP_ACTION, "fullscreen");
                if (tapAction.equals("start_stop") || tapAction.equals("button2"))
                    return;
                timeKeeper.copyToClipboard();
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (!Options.swipeEnabled(options) || e1 == null || e2 == null)
                    return false;

                final float baseSize;
                baseSize = Math.min(2.3f * metrics.xdpi, Math.min(bigDigits.getWidth(), bigDigits.getHeight()));

                final float minFlingPixels = baseSize * minimumFling;
                final float minV = baseSize * minimumVelocity;

                float dy = e2.getY() - e1.getY();
                float dx = e2.getX() - e1.getX();

//                debug("vx "+vx+" vy "+vy+ " baseSize " + baseSize);

                if (Math.abs(dx)>Math.abs(dy)*swipeAxisRatio && Math.abs(vx) > minV) {
                    if(-dx > minFlingPixels) {
                        flingLeft();
                        return true;
                    }
                    else if (dx > minFlingPixels) {
                        flingRight();
                        return true;
                    }
                }
                else if (Math.abs(dy)>Math.abs(dx)*swipeAxisRatio && Math.abs(vy) > minV) {
                    if(-dy > minFlingPixels) {
                        flingUp();
                        return true;
                    }
                    else if (dy > minFlingPixels) {
                        flingDown();
                        return true;
                    }
                }
                return false;
            }
        } );

        bigDigits.setOnTouchListener(gestureListener);
        bigDigits.post(new Runnable() {
            @Override
            public void run() {
                timeKeeper.updateViews();
            }
        });

        setVolumeControlStream(Options.getStream(options));

        setOrientation();

        setTheme();
        setFullScreen();
        debug("theme");
        int orientation = getResources().getConfiguration().orientation;

        debug("onResume");

        timeKeeper.restore();
        timeKeeper.updateViews();

        ImageButton settingsButton = findViewById(R.id.settings);
        ImageButton menuButton = findViewById(R.id.menu);
        int h = settingsButton.getPaddingTop()+(menuButton.getDrawable().getIntrinsicHeight()) * Options.extraHeight(options) / 200;
        menuButton.setPadding(0, h, 0, h);
        //menuButton.setMinimumHeight(h);
    }

    public void pressFirstButton() {
    }

    public void pressSecondButton() {
    }

    public void pressFirstButtonLong() {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        debug("onConfChanged");
        super.onConfigurationChanged(newConfig);
        timeKeeper.updateViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //timeKeeper.save();
        timeKeeper.stopUpdating();
        timeKeeper.suspend();
    }

    public void onButtonSettings(View view) {
        startActivity(new Intent(this, Options.class));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timeKeeper.destroy();
    }

    public static void debug(String s) {
        if (DEBUG)
            Log.v("chrono", s);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        setFullScreen();
    }

    public void lockOrientation() {
        if (!isTV() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            switch(((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation()) {
                case Surface.ROTATION_0:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
                case Surface.ROTATION_90:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;
                case Surface.ROTATION_180:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    }
                    break;
                case Surface.ROTATION_270:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    }
                    break;
            }
        }
    }

    public AlertDialog.Builder AlertDialog_Builder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return new AlertDialog.Builder(this, DARK_THEME);
        }
        else {
            return new AlertDialog.Builder(this);
        }
    }

    public void timedMessage(String title, String message, long time) {
        AlertDialog.Builder b = AlertDialog_Builder();
        b.setTitle(title);
        b.setMessage(message);
        final AlertDialog d = b.create();
        final Activity activity = this;
        d.show();
        new Handler().postDelayed(new Runnable(){
            public void run(){
                if (activity.isFinishing())
                    return;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())
                    return;
                try {
                    d.cancel();
                }
                catch(Exception e){}
                try {
                    d.dismiss();
                }
                catch(Exception e){}
            }
        }, time);
    }

    public void onButtonMenu(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            invalidateOptionsMenu();
        }
        try {
            openOptionsMenu();
        }
        catch(Exception e) {
            // on some devices openOptionsMenu() seems to crash
            // I have no idea why, but here is a workaround
            AlternateMenu.showAlternateMenu(this);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            onButtonMenu(null);
            return true;
        }
        else
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            flingUp();
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            flingDown();
            return true;
        }
        else if (event.getScanCode() == 513 || event.getScanCode() == 595) { // Sony camera delete
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
                finish();
            }
            else {
                finish();
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public static void clip(Context c, String s) {
        android.text.ClipboardManager clip = (android.text.ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE);
        clip.setText(s);
        Toast.makeText(c, "Copied", Toast.LENGTH_SHORT).show();
    }

    public static void vibrate(Activity context, long time) {
        if (time == 0)
            return;
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(time);
    }

    public boolean isTV() {
        if (Build.MODEL.startsWith("AFT")) {
            Application app = getApplication();
            String installerName = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ECLAIR) {
                installerName = app.getPackageManager().getInstallerPackageName(app.getPackageName());
            }
            if (installerName != null && installerName.equals("com.amazon.venezia"))
                return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            return getPackageManager().hasSystemFeature("android.hardware.type.television");
        }
        else {
            return false;
        }
    }

    public boolean hasTouch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            return getPackageManager().hasSystemFeature("android.hardware.touchscreen") && !isTV();
        }
        else {
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.go_settings:
                onButtonSettings(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void switchActivity(Class a, int direction) {
        SharedPreferences.Editor ed = options.edit();
        ed.putString(Options.PREF_LAST_ACTIVITY, a.getName());
        MyChrono.apply(ed);
        startActivity(new Intent(this, a));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            if (direction == RIGHT) {
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
                finish();
            }
            else if (direction == LEFT) {
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
                finish();
            }
            else if (direction == DOWN) {
                overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down);
                finish();
            }
            else if (direction == UP) {
                overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
                finish();
            }
            else if (direction == NONE) {
                overridePendingTransition(0,0);
                finish();
            }
        }
    }

    protected void flingLeft() {
        switchActivity(nextActivity(-1), LEFT);
    }

    protected void flingRight() {
        switchActivity(nextActivity(1), RIGHT);
    }

    protected void flingUp() {
        switchActivity(nextActivity(-1), UP);
    }

    protected void flingDown() {
        switchActivity(nextActivity(1), DOWN);
    }
}

class MyStateDrawable extends StateListDrawable {
    public MyStateDrawable(Context c, int id) {
        super();
        addState(new int[]{}, c.getResources().getDrawable(id));
    }

    @Override
    protected boolean onStateChange(int[] states) {
        boolean selected = false;
        for (int state : states) {
            if (state == android.R.attr.state_pressed || state == android.R.attr.state_focused) {
                selected = true;
                break;
            }
        }
        setAlpha(selected?128:255);
        return super.onStateChange(states);
    }

    @Override
    public boolean isStateful() {
        return true;
    }

}