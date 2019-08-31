package omegacentauri.mobi.simplestopwatch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class ShortTextView extends TextView {
    Rect bounds = new Rect();

    public ShortTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        String t = (String)getText();
        setText("");
        super.onDraw(canvas);
        setText(t);
        Paint p = getPaint();
        p.getTextBounds(t.replaceAll("[0-9]", "0"), 0, t.length(), bounds);
        canvas.drawText(t, (float)(canvas.getWidth()/2 - bounds.centerX()), (float)(canvas.getHeight()/2 - bounds.centerY()), p);
    }
}
