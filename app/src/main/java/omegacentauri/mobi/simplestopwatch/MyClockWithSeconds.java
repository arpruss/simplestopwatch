package omegacentauri.mobi.simplestopwatch;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import omegacentauri.mobi.simplestopwatch.MyClock;

public class MyClockWithSeconds extends MyClock {

    public MyClockWithSeconds(Activity context, SharedPreferences options, BigTextView mainView, TextView fractionView, View mainContainer) {
        super(context, options, mainView, fractionView, mainContainer);
    }

    public void restore() {

        twentyFourHour = options.getBoolean(Options.PREF_24HOUR, false);
        if (!twentyFourHour) {
            timeFormat = new SimpleDateFormat("h:mm:ss");
            fractionalFormat = new SimpleDateFormat("a");
        }
        else {
            timeFormat = new SimpleDateFormat("HH:mm:ss");
            fractionalFormat = new SimpleDateFormat("");
        }
        dateFormat = DateFormat.getDateInstance();
        startUpdating();
        updateViews();
    }


}
