package omegacentauri.mobi.simplestopwatch;
//
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.TabStopSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MyChrono implements BigTextView.GetCenter, MyTimeKeeper {
    private final Activity context;
    private final View mainContainer;
    private final BigTextView mainView;
    private String currentLapViewText;
    TextView fractionView;
    public long baseTime;
    public long pauseTime;
    public long delayTime;
    public String lapData;
    static final long MIN_DELAY_TIME = -60000;
    long lastLapTime;
    int vibrateAfterCountDown = 100;
    public boolean paused = false;
    public boolean active = false;
    boolean quiet = false;
    long lastAnnounced = 0;
    TextView lapView;
    Timer timer;
    int maxSize;
    Handler updateHandler;
    SharedPreferences options;
    public int precision = 100;
    private TextToSpeech tts = null;
    private boolean ttsMode;
    int STREAM = AudioManager.STREAM_ALARM;;
    private boolean boostAudio = false;
    private static final int GAIN = 2000;
    HashMap<String,String> ttsParams = new HashMap<String,String>();
    private LoudnessEnhancer loudnessEnhancer = null;
    private static final long SHORT_TONE_LENGTH = 75;
    private static final long LONG_TONE_LENGTH = 600;
    private static final float TONE_FREQUENCY = 2000;
    private AudioTrack shortTone;
    private AudioTrack longTone;

    @SuppressLint("NewApi")
    public MyChrono(Activity context, SharedPreferences options, BigTextView mainView, TextView fractionView, TextView lapView, View mainContainer) {
        this.mainView = mainView;
        this.context = context;
        this.options = options;
        this.mainContainer = mainContainer;
        this.lapData = "";
        this.delayTime = options.getLong(Options.PREF_DELAY, 0);
        this.fractionView = fractionView;
        this.lastLapTime = 0;
        this.lapView = lapView;
        this.lapView.setText("");
        this.currentLapViewText = "";
        this.lapView.setMovementMethod(new ScrollingMovementMethod());
        this.mainView.getCenterY = this;
        tts = null;

        StopWatch.debug("maxSize " +this.maxSize);

        updateHandler = new Handler() {
            public void handleMessage(Message m) {
                updateViews();
            }
        };
    }

    public long getTime() {
        return active ? (( paused ? pauseTime : SystemClock.elapsedRealtime() ) - baseTime) : delayTime;
    }

    private void announce(long t) {
//        long vibrate = Options.getVibration(options);
        if (/*(quiet &&  vibrate == 0) || */ !active || paused)
            return;
        if (t < -3000 || t >= 1000) {
            lastAnnounced = floorDiv(t, 1000)*1000;
        }
        else if (t < 0) {
            if (tts != null && ttsMode) {
                String msg;
                if (-1000 <= t) {
                    msg = "1";
                }
                else if (-2000 <= t) {
                    msg = "2";
                }
                else {
                    msg = "3";
                }
                StopWatch.debug("say: "+msg);
                tts.speak(msg,TextToSpeech.QUEUE_FLUSH, ttsParams);
            }
            else if (!quiet) {
                shortTone.stop();
                shortTone.reloadStaticData();
                shortTone.play();
            }
            lastAnnounced = floorDiv(t, 1000)*1000;
        }
        else if (t >= 0) {
            if (!quiet) {
                longTone.stop();
                longTone.reloadStaticData();
                longTone.play();
            }
            ShowTime.vibrate(context, vibrateAfterCountDown);
            lastAnnounced = 0;
        }
    }

    public void updateViews() {
        long t = getTime();
        if (lastAnnounced < 0 && lastAnnounced + 1000 <= t) {
            announce(t+10);
        }

        if (lapData.length() == 0) {
            if (lapView.getVisibility() != View.GONE)
                lapView.setVisibility(View.GONE);
        }
        else {
            String adjLapData;
            if (paused && active && lapData.length() > 0) {
                adjLapData = lapData + "\n" + formatLapTime(pauseTime-baseTime);
            }
            else {
                adjLapData = lapData;
            }

            if (lapView.getVisibility() == View.GONE || ! currentLapViewText.equals(adjLapData)) {
                SpannableStringBuilder span = new SpannableStringBuilder(adjLapData);
                int w1 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 30, context.getResources().getDisplayMetrics());
                int w2 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 110, context.getResources().getDisplayMetrics());
                span.setSpan(new TabStopSpan.Standard(w1), 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
                span.setSpan(new TabStopSpan.Standard(w2), 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );

                lapView.setText(span);
                currentLapViewText = adjLapData;
                lapView.setMaxLines(Math.min(5,getNumberOfLaps()+(paused?1:0)));
                lapView.setVisibility(View.VISIBLE);
//                this.lapView.setMovementMethod(new ScrollingMovementMethod());
            }
        }

        mainView.setText(formatTime(t,mainView.getHeight() > mainView.getWidth()));
        setFractionView(formatTimeFraction(t, active && paused, false));

    }

    private void setFractionView(String s) {
        fractionView.setText(s);
        fractionView.setTextScaleX(1f);
        float w = fractionView.getPaint().measureText(s, 0, s.length());
        float wCur = fractionView.getWidth() * 0.98f - 2;
        if (wCur <= 0) {
            fractionView.setText("");
            return;
        }
        if (w > wCur)
            fractionView.setTextScaleX(wCur/w);
        fractionView.setText(s);
    }

    static final long floorDiv(long a, long b) {
        long q = a/b;
        if (q*b > a)
            return q-1;
        else
            return q;
    }

    String formatTime(long t, boolean multiline) {
        //t+=1000*60*60*60;
        if (t<0)
            return String.format("\u2212%d", -floorDiv(t,1000));
        String format = options.getString(Options.PREF_FORMAT, "h:m:s");
        boolean fraction = format.endsWith(".ms");
        String suffix;
        if (fraction) {
            format = format.substring(0, format.lastIndexOf('.'));
            if (precision == 100) {
                suffix = String.format(".%01d", (t / 100) % 10);
            }
            else if (precision == 10) {
                suffix = String.format(".%02d", (t / 10) % 100);
            }
            else if (precision == 1) {
                suffix = String.format(".%03d", t % 1000);
            }
            else {
                suffix = "";
            }
        }
        else {
            suffix = "";
        }

        t /= 1000;
        if (format.equals("s")) {
            return String.format("%02d", t) + suffix;
        }
        if (format.endsWith("m")) {
            t /= 60;
            if (format.equals("m"))
                return String.format("%d", t) + suffix;
            else if (format.equals("mm"))
                return String.format("%02d", t) + suffix;
        }
        int part0 = (int) (t % 60); // seconds or minutes
        t /= 60;
        int part1;
        int part2;
        if (format.equals("h:m:s")) {
            part1 = (int) (t % 60); // minutes
            t /= 60;
            part2 = (int) t; // hours
        }
        else {
            part1 = (int) t; // seconds or minutes
            part2 = 0;
        }
        if (multiline) {
            Boolean threeLine = options.getBoolean(Options.PREF_THREE_LINE, true);
            if (part2 != 0) {
                if (threeLine)
                    return String.format("%02d\n%02d\n%02d", part2, part1, part0) + suffix;
                else
                    return String.format("%d:%02d\n%02d", part2, part1, part0) + suffix;
            }
            else
                return String.format("%02d\n%02d", part1, part0) + suffix;
        }
        else {
            if (part2 != 0)
                return String.format("%d:%02d:%02d", part2, part1, part0) + suffix;
            else
                return String.format("%d:%02d", part1, part0) + suffix;
        }
    }

    String formatTimeFraction(long t, boolean greaterPrecision, boolean includeColonIfNeeded) {
        if (t<0)
            return "";
        String seconds;
        String format = options.getString(Options.PREF_FORMAT, "h:m:s");
        if (format.endsWith(".ms"))
            return "";
        if (format.endsWith("m")) {
            seconds = (includeColonIfNeeded ? ":" : "") + String.format("%02d", (t/1000)%60);
        }
        else {
            seconds = "";
        }
        if (precision == 10 || (precision > 10 && greaterPrecision))
            return seconds+String.format(".%02d", (int)((t / 10) % 100));
        else if (precision == 100)
            return seconds+String.format(".%01d", (int)((t / 100) % 10));
        else if (precision == 1)
            return seconds+String.format(".%03d", (int)(t % 1000));
        else
            return seconds;
    }

    String formatTimeFull(long t) {
        if (t<0) {
            if (precision == 1)
                return String.format("%.03f", (float)(t/1000.));
            else
                return String.format("%.02f", (float)(t/1000.));
        }
        return formatTime(t, false)+formatTimeFraction(t,true ,true);
    }

    public int getNumberOfLaps() {
        if (lapData.length() == 0)
            return 0;
        return lapData.split("\\n").length;
    }

    private String formatLapTime(long t) {
        return String.format("#%d\t%s\t%s", getNumberOfLaps() + 1, formatTimeFull(t), formatTimeFull(t-lastLapTime));
    }

    public void secondButton() {
        if (active && ! paused) {
            long t = getTime();
            if (0 <= t) {
                String l = formatLapTime(t);
                if (lapData.length() > 0)
                    lapData += "\n" + l;
                else {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                        context.invalidateOptionsMenu();
//                    }
                    lapData = l;
                }
                lastLapTime = t;
            }
            updateViews();
        }
        else if (active) {
            active = false;
            lapData = "";
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                context.invalidateOptionsMenu();
//            }
            lastLapTime = 0;
            stopUpdating();
        }
        else {
            long delayTimes[];
            long cd = Options.getCustomDelay(options);
            if (cd == 0 || cd == 3000 || cd == 6000 || cd == 9000)
                delayTimes = new long[] { 0, 3000, 6000, 9000 };
            else {
                delayTimes = new long[] { 0, 3000, 6000, 9000, cd };
                Arrays.sort(delayTimes);
            }
            boolean didSet = false;
            for (int i = 0; i < delayTimes.length; i++) {
                if (delayTimes[i] == -delayTime) {
                    delayTime = -delayTimes[(i+1) % delayTimes.length];
                    didSet = true;
                    break;
                }
            }
            if (!didSet)
                delayTime = 0;
        }
        save();
        updateViews();
    }

    private short[] sinewave(float frequency, long duration) {
        int numSamples = (int)(44.100 * duration);
        double alpha = frequency / 44100 * 2 * Math.PI;
        short[] samples = new short[numSamples];
        for (int i = 0 ; i < numSamples ; i++)
            samples[i] = (short) (32767. * Math.sin(alpha * i));
        return samples;
    }

    public void firstButton() {
        if (active && paused) {
            baseTime += SystemClock.elapsedRealtime() - pauseTime;
            paused = false;
            startUpdating();
            save();
        }
        else if (!active) {
            lastLapTime = 0;
            if (delayTime < 0)
                lastAnnounced = delayTime - 1000;
            else
                lastAnnounced = 1000;
            baseTime = SystemClock.elapsedRealtime() - delayTime;
            paused = false;
            active = true;
            startUpdating();
            save();
        }
        else {
            paused = true;
            pauseTime = SystemClock.elapsedRealtime();
            stopUpdating();
            save();
        }
        updateViews();
    }

    public void setAudio(String soundMode) {
        boostAudio = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && options.getBoolean(Options.PREF_BOOST, false);
        STREAM = Options.getStream(options);

        vibrateAfterCountDown = options.getBoolean(Options.PREF_VIBRATE_AFTER_COUNTDOWN, true) ? (int)LONG_TONE_LENGTH/2 : 0;

        if (soundMode.equals("none")) {
            quiet = true;
            ttsMode = false;
            return;
        }

        quiet = false;

        short[] tone = sinewave(TONE_FREQUENCY, LONG_TONE_LENGTH);
        longTone = new AudioTrack(STREAM, 44100, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, tone.length * 2, AudioTrack.MODE_STATIC);
        longTone.write(tone, 0, tone.length);
        int sessionId = 0;
        int shortLength = Math.min(tone.length, (int) (44.100 * SHORT_TONE_LENGTH));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
            sessionId = longTone.getAudioSessionId();
            shortTone = new AudioTrack(STREAM, 44100, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, shortLength * 2, AudioTrack.MODE_STATIC, sessionId);
        }
        else {
            shortTone = new AudioTrack(STREAM, 44100, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, shortLength * 2, AudioTrack.MODE_STATIC);
        }
        shortTone.write(tone, 0, shortLength);
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (options.getBoolean(Options.PREF_BOOST, false))
            am.setStreamVolume(STREAM, am.getStreamMaxVolume(STREAM), 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && boostAudio) {
            try {
                StopWatch.debug("trying to boost");
                loudnessEnhancer = new LoudnessEnhancer(sessionId);
                loudnessEnhancer.setTargetGain(GAIN);
                loudnessEnhancer.setEnabled(true);
                if (!loudnessEnhancer.getEnabled()) {
                    loudnessEnhancer = null;
                }
                else {
                    StopWatch.debug("loudness success");
                }
            }
            catch(Exception e) {
            }
        }

        if (soundMode.equals("voice")) {
            ttsMode = true;
            ttsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(STREAM));
            tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status != TextToSpeech.SUCCESS){
                        tts = null;
                    }
                    else {
                        tts.speak(" ",TextToSpeech.QUEUE_FLUSH, ttsParams);
                    }
                }
            });
            if (loudnessEnhancer != null)
                ttsParams.put(TextToSpeech.Engine.KEY_PARAM_SESSION_ID, String.valueOf(sessionId));
        }
        else if (soundMode.equals("beeps")) {
            ttsMode = false;
        }
    }

    public void restore() {
        baseTime = options.getLong(Options.PREF_START_TIME, 0);
        pauseTime = options.getLong(Options.PREF_PAUSED_TIME, 0);
        delayTime = options.getLong(Options.PREF_DELAY, 0);
        active = options.getBoolean(Options.PREF_ACTIVE, false);
        paused = options.getBoolean(Options.PREF_PAUSED, false);
        lapData = options.getString(Options.PREF_LAPS, "");
        lastLapTime = options.getLong(Options.PREF_LAST_LAP_TIME, 0);
        lastAnnounced = options.getLong(Options.PREF_LAST_ANNOUNCED, 0);
        setAudio(options.getString(Options.PREF_SOUND, "voice"));

        StopWatch.debug("baseTime "+baseTime);

        precision = Integer.parseInt(options.getString(Options.PREF_PRECISION, "100"));
        if (SystemClock.elapsedRealtime() < baseTime + MIN_DELAY_TIME) {
            StopWatch.debug("deactivating");
            active = false;
        }

        if (options.getBoolean(Options.PREF_BOOT_ADJUSTED, false)) {
            SharedPreferences.Editor ed = options.edit();
            ed.putBoolean(Options.PREF_BOOT_ADJUSTED, false);
            apply(ed);
            StopWatch.debug("adjusted boot warn?");
            if (active && !paused) {
                StopWatch.debug("yeah");
                Toast.makeText(context, "Reboot detected: Some precision may be lost", Toast.LENGTH_LONG).show();
            }
        }

        if (active && !paused) {
            startUpdating();
        }
        else {
            stopUpdating();
        }
        updateViews();
    }

    public static void apply(SharedPreferences.Editor ed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            ed.apply();
        }
        else {
            ed.commit();
        }
    }

    public static long getBootTime() {
        return java.lang.System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime();
    }

    public void save() {
        StopWatch.debug("saving");
        SharedPreferences.Editor ed = options.edit();
        ed.putLong(Options.PREF_START_TIME, baseTime);
        ed.putLong(Options.PREF_PAUSED_TIME, pauseTime);
        ed.putBoolean(Options.PREF_ACTIVE, active);
        ed.putBoolean(Options.PREF_PAUSED, paused);
        ed.putLong(Options.PREF_DELAY, delayTime);
        ed.putString(Options.PREF_LAPS, lapData);
        ed.putLong(Options.PREF_LAST_LAP_TIME, lastLapTime);
        ed.putLong(Options.PREF_LAST_ANNOUNCED, lastAnnounced);
        ed.putLong(Options.PREF_BOOT_TIME, getBootTime());

        apply(ed);
    }

    public void stopUpdating() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        ((Activity)context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void startUpdating() {
//        if (delayTime < 0 && lastAnnounced < 0 && !quiet) {
//        }
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    updateHandler.obtainMessage(1).sendToTarget();
                }
            }, 0, precision<=10 ? precision : 50); // avoid off-by-1 errors at lower precisions, at cost of some battery life
        }
        if (options.getBoolean(Options.PREF_SCREEN_ON, true))
            ((Activity)context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            ((Activity)context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static void clearSaved(SharedPreferences pref) {
        SharedPreferences.Editor ed = pref.edit();
        ed.putBoolean(Options.PREF_ACTIVE, false);
        apply(ed);
        StopWatch.debug("cleared "+Options.PREF_ACTIVE);
    }

    public static void detectBoot(SharedPreferences options) {
        return;
        /*
        long bootTime = java.lang.System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime();
        long oldBootTime = options.getLong(Options.PREF_BOOT_TIME, -100000);
        SharedPreferences.Editor ed = options.edit();
        if (Math.abs(oldBootTime-bootTime)>60000) {
            ed.putBoolean(Options.PREF_ACTIVE, false);
        }
        ed.putLong(Options.PREF_BOOT_TIME, bootTime);
        ed.apply(); */
    }

    public void copyToClipboard() {
        StopWatch.clip(context, formatTimeFull(getTime()));
    }

    public void copyLapsToClipboard() {
        StopWatch.clip(context, currentLapViewText.replace('\t', ' '));
//            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT);
    }

    public void clearLapData() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//            context.invalidateOptionsMenu();
//        }
        lapData = "";
        lastLapTime = 0;
        save();
        updateViews();
    }

    public void suspend() {
        destroyAudio();
    }

    public void destroy() {
        destroyAudio();
    }

    public void destroyAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && loudnessEnhancer != null) {
            loudnessEnhancer.setEnabled(false);
            loudnessEnhancer = null;
        }
        if (shortTone != null) {
            shortTone.release();
            shortTone = null;
        }
        if (longTone != null) {
            longTone.release();
            longTone = null;
        }
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }

    }

    public static void fixOnBoot(SharedPreferences options) {
        if (! options.getBoolean(Options.PREF_ACTIVE, false)) {
            StopWatch.debug("not active");
            return;
        }
        long oldBootTime = options.getLong(Options.PREF_BOOT_TIME, 0);
        SharedPreferences.Editor ed = options.edit();
        if (oldBootTime == 0) {
            StopWatch.debug("zero old boot time");
            ed.putBoolean(Options.PREF_ACTIVE, false);
        }
        else {
            long delta = getBootTime() - oldBootTime;
            if (delta == 0)
                return;
            adjust(options, ed, Options.PREF_START_TIME, -delta);
            adjust(options, ed, Options.PREF_PAUSED_TIME, -delta);
            ed.putBoolean(Options.PREF_BOOT_ADJUSTED, true);
        }
        apply(ed);
    }

    private static void adjust(SharedPreferences options, SharedPreferences.Editor ed, String opt, long delta) {
        StopWatch.debug("opt "+opt+" "+delta);
        ed.putLong(opt, options.getLong(opt, 0) + delta);
    }

    @Override
    public float getCenter() {
        return mainContainer.getHeight() / 2f;
    }
}
