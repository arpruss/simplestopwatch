package omegacentauri.mobi.simplestopwatch;

import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Float.max;

public class MyChrono {
    TextView mainView;
    TextView fractionView;
    long baseTime;
    long pauseTime;
    boolean paused;
    TextView view;
    Timer timer;
    TimerTask update;
    public int precision;

    String formatTime(long t) {
        t /= 1000;
        int s = (int) (t % 60);
        t /= 60;
        int m = (int) (t % 60);
        t /= 60;
        int h = (int) t;
        if (h != 0)
            return String.format("%d:%02d:%02d", h, m, s);
        else
            return String.format("%02d:%02d", m, s);
    }

    String formatTimeFraction(long t) {
        if (precision == 100)
            return String.format(".%1d", (int)((t / 100) % 10));
        else if (precision == 10)
            return String.format(".%2d", (int)((t / 10) % 100));
        else if (precision == 1)
            return String.format(".%3d", (int)(t % 1000));
        else
            return "";
    }

    public MyChrono(final TextView mainView, final TextView fractionView, Timer timer) {
        paused = true;
        baseTime = SystemClock.elapsedRealtime();
        pauseTime = baseTime;
        this.mainView = mainView;
        this.fractionView = fractionView;

        update = new TimerTask() {

            @Override
            public void run() {
                long t = ( paused ? pauseTime : SystemClock.elapsedRealtime() ) - baseTime;
                String main = formatTime(t);
                String fraction = formatTimeFraction(t);
                MyChrono.this.mainView.setText(main);
                maximizeSize(MyChrono.this.mainView);
                MyChrono.this.fractionView.setText(fraction);

            }
        };
    }

    public static void maximizeSize(TextView v) {
        String s = ((String)v.getText()).replaceAll("[0-9]", "8");
        if (s.length() == 0)
            return;
        Paint p = new Paint(v.getPaint());
        p.setTextSize(50);
        Rect bounds = new Rect();
        p.getTextBounds((String)s, 0, s.length(), bounds);
        float textWidth = bounds.width();
        float textHeight = bounds.height();

        if (v.getWidth() > 0 && v.getHeight() > 0) {
            try {
                float scale = Math.min(v.getWidth() / (float)bounds.width(), v.getHeight() / (float)bounds.height());
                float newSize = (float) (p.getTextSize()*scale*0.9);
                if (Math.abs(newSize - v.getTextSize()) > 10) {
                    v.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);
                }
            }
            catch(Exception e) {}
        }
    }

    public void stop() {
        timer.purge();
    }

}
