package omegacentauri.mobi.simplestopwatch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

public class ShortTextView extends TextView {
    Rect bounds = new Rect();
    public float lineSpacing = 1.05f;

    public ShortTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void measureText(Rect bounds) {
        Paint p = getPaint();
        String[] lines = ((String)getText()).replaceAll("[0-9]", "0").split("\\n");
        Rect lineBounds = new Rect();
        bounds.set(0,0,0,0);
        int n = lines.length;
        for (int i = 0 ; i < n ; i++) {
            String line = lines[i];
            p.getTextBounds(line, 0, line.length(), lineBounds);
            bounds.bottom += (int)(lineBounds.height() * (i==n-1 ? 1 : lineSpacing));
            bounds.right = Math.max(bounds.right, lineBounds.width());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint p = getPaint();
        String t = (String)getText();
        setText("");
        super.onDraw(canvas);
        setText(t);
        String[] lines = t.split("\\n");
        int n = lines.length;
        if (n==0)
            return;
        int yOffsets[] = new int[n];
        int xOffsets[] = new int[n];
        int height = 0;
        for (int i = 0 ; i < n ; i++) {
            String line = lines[i];
            p.getTextBounds(line.replaceAll("[0-9]", "0"), 0, line.length(), bounds);
            yOffsets[i] = height - bounds.top;
            height += (int)(bounds.height() * (i==n-1 ? 1 : lineSpacing));
            xOffsets[i] = canvas.getWidth()/2 - bounds.centerX();
        }
        int dy = canvas.getHeight()/2 - height/2;
        for (int i = 0 ; i < n ; i++) {
            canvas.drawText(lines[i], xOffsets[i], dy + yOffsets[i], p);
        }
    }
}
