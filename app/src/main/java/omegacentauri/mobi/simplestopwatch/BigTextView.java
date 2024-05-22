package omegacentauri.mobi.simplestopwatch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
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
    float yOffsets[];
    float xOffsets[];
    float scale = 0.98f;
    GetCenter getCenterX = null;
    GetCenter getCenterY = null;
    static final float BASE_FONT_SIZE = 50f;
    private float maxAspect = 1f;
    private double dimFraction;
    private Paint dimPaint;
    private Paint currentPaint;

    public BigTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        miniFont = new SansDigitsColon();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);

        dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dimPaint.setStyle(Paint.Style.FILL);
        dimPaint.setColor(Color.BLACK);

        dimFraction = 1.0;
        currentPaint = paint;

        basePaint = new Paint();
        basePaint.setTextSize(BASE_FONT_SIZE);

        setText("", true, false);
    }

    public void setDimFraction(double f) {
        dimFraction = f;
        dimPaint.setColor(dim(paint.getColor()));
        invalidate();
    }

    public void setMaxAspect(float maxAspect) {
        if (this.maxAspect != maxAspect) {
            this.maxAspect = maxAspect;
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

    public void setLineSpacing(float l) {
        lineSpacing = l;
        invalidate();
    }

    public void setLetterSpacing(float ls) {
        letterSpacing = ls;
        invalidate();
    }

/*    public void setText(String s, boolean b, boolean b1) {
        setText(s, false);
    } */

    public void setText(String s, Boolean force, Boolean paused) {
        Paint newPaint = paused ? dimPaint : paint;

        if (!force && text != null && text.equals(s) && currentPaint == newPaint)
            return;

        currentPaint = newPaint;

        text = new String(s);

        lines = text.split("\\n");

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

        int n = lines.length;
        if (n==0)
            return;

        float height = 0;
        float cWidth = this.getWidth(); // canvas.getWidth();
        float cHeight = this.getHeight(); // canvas.getHeight();
        float cx = cWidth / 2f;
        float maxWidth = 0;

        for (int i = 0 ; i < n ; i++) {
            String line = lines[i];
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

        float adjust = scale * Math.max(cWidth / maxWidth, cHeight / height);
        if (adjust * maxWidth > cWidth * scale)
            adjustX = scale * cWidth / maxWidth;
        else
            adjustX = adjust;
        if (adjust * height > cHeight * scale)
            adjustY = scale * cHeight / height;
        else
            adjustY = adjust;

        if (adjustX > maxAspect * adjustY)
            adjustX = maxAspect * adjustY;
        else if (adjustY > maxAspect * adjustX)
            adjustY = maxAspect * adjustX;

        currentPaint.setTextSize(adjustY * BASE_FONT_SIZE);
        currentPaint.setTextScaleX(adjustX / adjustY);

        float dy = cHeight/2f - adjustY * height/2f;

        cx = adjustCenter(cWidth, getCenterX, adjustX * maxWidth);
        dy += adjustCenter(cHeight, getCenterY, adjustY * height) - cHeight/2f;

        for (int i = 0 ; i < n ; i++) {
            miniFont.drawText(canvas, lines[i], cx + adjustX * xOffsets[i], dy + adjustY * yOffsets[i], currentPaint, letterSpacing);
        }
    }

    private float adjustCenter(float canvasSize, GetCenter c, float size) {
        float canvasCenter = canvasSize / 2f;
        if (c == null)
            return canvasCenter;
        float drawingCenter = c.getCenter();
        if (canvasCenter == drawingCenter)
            return canvasCenter;
        float half = size / 2f;
        if (drawingCenter < canvasCenter) {
            if (half <= drawingCenter)
                return drawingCenter;
            else
                return half;
        }
        else {
            if (drawingCenter + half <= canvasSize) {
                return drawingCenter;
            }
            else
                return canvasSize - half;
        }
    }

    private int dim(int color) {
        int a = (color >> 24) & 0xff;
        int r = (color >> 16) & 0xff;
        int g = (color >>  8) & 0xff;
        int b = (color      ) & 0xff;
        if (r == 0 && g == 0 && b == 0) {
            r = (int) (255 * (1-dimFraction));
            g = r;
            b = r;
        }
        else {
            StopWatch.debug("color "+r+" "+g+" "+b);
            r = (int) (r * dimFraction);
            g = (int) (g * dimFraction);
            b = (int) (b * dimFraction);
            StopWatch.debug("new color "+r+" "+g+" "+b);
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public void setTextColor(int textColor) {
        if (textColor != paint.getColor()) {
            paint.setColor(textColor);
            dimPaint.setColor(dim(textColor));
            invalidate();
        }
    }

    static interface GetCenter {
        float getCenter();
    }
}
