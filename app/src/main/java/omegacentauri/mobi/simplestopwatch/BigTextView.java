package omegacentauri.mobi.simplestopwatch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

public class BigTextView extends View {
    RectF bounds = new RectF();
    float lineSpacing = 1.05f;
    final MiniFont mf = new SansDigitsColon();
    Paint paint;
    String text;
    String replacedText;
    String[] lines;
    String[] tweakedLines;
    Boolean equalizeDigits;
    float yOffsets[];
    float xOffsets[];

    public BigTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);

        equalizeDigits = true;

        setText("", true);
    }

    public void setTextSizePixels(float size) {
        if (size != paint.getTextSize()) {
            paint.setTextSize(size);
            invalidate();
        }
    }

    public void setEqualizeDigits(Boolean v) {
        equalizeDigits = v;
        setText(text, true);
    }

    public void setLineSpacing(float l) {
        lineSpacing = l;
        invalidate();
    }

    public void setText(String s) {
        setText(s, false);
    }

    public void setText(String s, Boolean force) {
        if (!force && text != null && text.equals(s))
            return;

        text = new String(s);

        lines = text.split("\\n");
        tweakedLines = equalizeDigits ? text.replaceAll("[0-9]", "0").split("\\n") : lines;

        int n = lines.length;
        xOffsets = new float[n];
        yOffsets = new float[n];

        postInvalidate();
    }

    public void measureText(RectF bounds) {
        RectF lineBounds = new RectF();
        bounds.set(0,0,0,0);
        int n = tweakedLines.length;
        for (int i = 0 ; i < n ; i++) {
            String line = tweakedLines[i];
            mf.getTextBounds(paint, line, 0, line.length(), lineBounds);
            bounds.bottom += (int)(lineBounds.height() * (i==n-1 ? 1 : lineSpacing));
            bounds.right = Math.max(bounds.right, lineBounds.width());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int n = lines.length;
        if (n==0)
            return;
        int height = 0;
        for (int i = 0 ; i < n ; i++) {
            String line = tweakedLines[i];
            mf.getTextBounds(paint, line, 0, line.length(), bounds);
            yOffsets[i] = height - bounds.top;
            height += (int)(bounds.height() * (i==n-1 ? 1 : lineSpacing));
            xOffsets[i] = canvas.getWidth()/2 - bounds.centerX();
        }
        int dy = canvas.getHeight()/2 - height/2;
        for (int i = 0 ; i < n ; i++) {
            mf.drawText(canvas, lines[i], xOffsets[i], dy + yOffsets[i], paint);
        }
    }

    public void setTextColor(int textColor) {
        if (textColor != paint.getColor()) {
            paint.setColor(textColor);
            invalidate();
        }
    }

    public float getTextSizePixels() {
        return paint.getTextSize();
    }
}
