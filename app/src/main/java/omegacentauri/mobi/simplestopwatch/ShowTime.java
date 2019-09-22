package omegacentauri.mobi.simplestopwatch;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

abstract public class ShowTime extends Activity {
    private static final boolean DEBUG = true;
    private static final int DARK_THEME = Build.VERSION.SDK_INT >= 23 ?
            android.R.style.Theme_DeviceDefault_Dialog_Alert :
            Build.VERSION.SDK_INT >= 14 ?
                    AlertDialog.THEME_DEVICE_DEFAULT_DARK :
                    4;
    SharedPreferences options;
    private float unselectedThickness = 2f;
    private float focusedThickness = 6f;
    private float selectedThickness = 6f;
    protected View mainContainer;
    protected static int textButtons[] = {};
    protected static int imageButtons[][] = {};
    protected View controlBar;
    protected static final int NONE = 0;
    protected static final int LEFT = 1;
    protected static final int RIGHT = 2;
    protected static final int DOWN = 3;
    protected static final int UP = 4;

//    protected View.OnClickListener fullScreenListener;
    public BigTextView bigDigits;
    protected MyTimeKeeper timeKeeper;
    private GestureDetector gestureDetector;
    protected View.OnTouchListener gestureListener;

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

/*        fullScreenListener = new View.OnClickListener() {
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
        }; */

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                debug("singleTapUp");
                if (!options.getBoolean(Options.PREF_CONTROL_FULLSCREEN, true))
                    return false;
                SharedPreferences.Editor ed = options.edit();
                ed.putBoolean(Options.PREF_FULLSCREEN, ! options.getBoolean(Options.PREF_FULLSCREEN, false));
                MyChrono.apply(ed);
                setFullScreen();
                setTheme();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {
                debug("longPress");
                timeKeeper.copyToClipboard();
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (Math.abs(e1.getY() - e2.getY()) < 250 && Math.abs(vx)>200) {
                    if(e1.getX() - e2.getX() > 120) {
                        flingLeft();
                        return true;
                    }
                    else if (e2.getX() - e1.getX() > 120) {
                        flingRight();
                        return true;
                    }
                }
                else if (Math.abs(e1.getX() - e2.getX()) < 250 && Math.abs(vy)>200) {
                    if(e1.getY() - e2.getY() > 120) {
                        flingUp();
                        return true;
                    }
                    else if (e2.getY() - e1.getY() > 120) {
                        flingDown();
                        return true;
                    }
                }
                return false;
            }
        } );
        gestureListener = new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                debug("onTouch "+motionEvent);
                return gestureDetector.onTouchEvent(motionEvent);
            }
        };
    }

    abstract protected void flingLeft();
    abstract protected void flingRight();
    abstract protected void flingUp();
    abstract protected void flingDown();

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

        int fore = Options.getForeColor(options);
        int controlFore = getControlBarForeColor();
        int back = Options.getBackColor(options);

        ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0).setBackgroundColor(back);

        ((TextView)findViewById(R.id.fraction)).setTextColor(controlFore);
        debug(String.format("controlFore=%x", controlFore));

        bigDigits.setTextColor(fore);

        for (int id : textButtons) {
            Button b = findViewById(id);
            b.setTextColor(controlFore);
            b.setBackgroundDrawable(makeOvalButtonBackground(controlFore));
        }

        for (int[] ids : imageButtons) {
            ImageButton b = findViewById(ids[0]);
            b.setImageDrawable(new MyStateDrawable(this, ids[1]));
            b.setColorFilter(controlFore, PorterDuff.Mode.MULTIPLY);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(back);
            View dv = window.getDecorView();
            int flags = dv.getSystemUiVisibility();
            if (back == Color.WHITE)
                dv.setSystemUiVisibility(flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR /* | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR */);
            else
                dv.setSystemUiVisibility(flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR /* & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR */);
        }
    }

    void setOrientation() {
        if (isTV())
            return;
        String o = options.getString(Options.PREF_ORIENTATION, "automatic");
        if (o.equals("landscape"))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else if (o.equals("portrait"))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

    }

    protected void setFullScreen()
    {
        boolean fs = options.getBoolean(Options.PREF_CONTROL_FULLSCREEN, true) && options.getBoolean(Options.PREF_FULLSCREEN, false);
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
        mainContainer.setLayoutParams(lp);
    }

    @Override
    protected void onResume() {
        super.onResume();

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
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    break;
                case Surface.ROTATION_270:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
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
        d.show();
        new Handler().postDelayed(new Runnable(){
            public void run(){
                d.cancel();
                d.dismiss();
            }
        }, time);
    }

    public void onButtonMenu(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            invalidateOptionsMenu();
        }
        openOptionsMenu();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            onButtonMenu(null);
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            flingUp();
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            flingDown();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public static void clip(Context c, String s) {
        android.text.ClipboardManager clip = (android.text.ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE);
        clip.setText(s);
    }

    public static void vibrate(Activity context, long time) {
        if (time == 0)
            return;
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(time);
    }

    public boolean isTV() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            return getPackageManager().hasSystemFeature("android.hardware.type.television");
        }
        else {
            return false;
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