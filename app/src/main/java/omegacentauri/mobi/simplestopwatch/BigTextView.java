package omegacentauri.mobi.simplestopwatch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class BigTextView extends View {
    RectF bounds = new RectF();
    float lineSpacing = 1.05f;
    float letterSpacing = 1f;
    MiniFont miniFont;
    Paint paint;
    Paint basePaint;
    String text;
    String[] lines;
    String[] tweakedLines;
    Boolean equalizeDigits = true;
    Boolean keepAspect = true;
    float yOffsets[];
    float xOffsets[];
    float scale = 0.96f;
    static final float BASE_FONT_SIZE = 50f;

    public BigTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        miniFont = new SansDigitsColon();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);

        basePaint = new Paint();
        basePaint.setTextSize(BASE_FONT_SIZE);

        setText("", true);
    }

    public void setKeepAspect(boolean keepAspect) {
        if (this.keepAspect != keepAspect) {
            this.keepAspect = keepAspect;
            invalidate();
        }
    }

    public void setFont(MiniFont mf) {
        this.miniFont = mf;
        invalidate();
    }

    public void setScale(float scale) {
        this.scale = scale;
        invalidate();
    }

    public void setEqualizeDigits(Boolean v) {
        equalizeDigits = v;
        setText(text, true);
    }

    public void setLineSpacing(float l) {
        lineSpacing = l;
        invalidate();
    }

    public void setLetterSpacing(float ls) {
        letterSpacing = ls;
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

        invalidate();
    }

    /*
    public void measureText(RectF bounds) {
        RectF lineBounds = new RectF();
        bounds.set(0,0,0,0);

        int n = tweakedLines.length;
        for (int i = 0 ; i < n ; i++) {
            String line = tweakedLines[i];
            miniFont.getTextBounds(paint, line, 0, line.length(), lineBounds);
            yOffsets[i] = bounds.bottom - lineBounds.top;
            bounds.bottom += (int)(lineBounds.height() * (i==n-1 ? 1 : lineSpacing));
            bounds.right = Math.max(bounds.right, lineBounds.width());
        }
    }
    */

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Log.v("chrono", "onDraw" + text);

        int n = lines.length;
        if (n==0)
            return;

        float height = 0;
        float cWidth = canvas.getWidth();
        float cHeight = canvas.getHeight();
        float cx = cWidth / 2f;
        float maxWidth = 0;

        for (int i = 0 ; i < n ; i++) {
            String line = tweakedLines[i];
            miniFont.getTextBounds(basePaint, letterSpacing, line, 0, line.length(), bounds);
            yOffsets[i] = height - bounds.top;
            height += bounds.height() * (i==n-1 ? 1 : lineSpacing);
            xOffsets[i] = -bounds.centerX();
            maxWidth = Math.max(maxWidth, bounds.width());
        }

        if (maxWidth == 0 || height == 0)
            return;

        float adjustX;
        float adjustY;

        if (keepAspect) {
            adjustX = scale * Math.min(cWidth / maxWidth, cHeight / height);
            adjustY = adjustX;
        }
        else {
            adjustX = scale * cWidth / maxWidth;
            adjustY = scale * cHeight / height;
        }

        paint.setTextSize(adjustY * BASE_FONT_SIZE);
        paint.setTextScaleX(adjustX / adjustY);

        float dy = cHeight/2f - adjustY * height/2f;

        for (int i = 0 ; i < n ; i++) {
            miniFont.drawText(canvas, lines[i], cx + adjustX * xOffsets[i], dy + adjustY * yOffsets[i], paint, letterSpacing);
        }
    }

    public void setTextColor(int textColor) {
        if (textColor != paint.getColor()) {
            paint.setColor(textColor);
            invalidate();
        }
    }
}