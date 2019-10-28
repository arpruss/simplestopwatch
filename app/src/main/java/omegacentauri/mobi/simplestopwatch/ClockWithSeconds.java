package omegacentauri.mobi.simplestopwatch;

import android.view.Menu;
import android.widget.TextView;

public class ClockWithSeconds extends Clock {
    @Override
    protected void setupChrono() {
        chrono = new MyClockWithSeconds(this, options, bigDigits, (TextView) findViewById(R.id.fraction),
                mainContainer);
        timeKeeper = chrono;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.clockwithsecondskmenu, menu);
        return true;
    }

}
