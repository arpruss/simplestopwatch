package omegacentauri.mobi.simplestopwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class boot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(context);
        MyChrono.clearSaved(options, "");
    }
}

