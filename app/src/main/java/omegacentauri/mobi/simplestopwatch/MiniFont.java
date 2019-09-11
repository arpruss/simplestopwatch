package omegacentauri.mobi.simplestopwatch;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

abstract public class MiniFont {
    public float defaultFontSize = 1;
    public Map<Character, Glyph> map;
    public boolean maximizeDigitBounds;

    public MiniFont(boolean maximizeDigitBounds) {
        map = new HashMap<Character, Glyph>();
        addFontData();
        this.maximizeDigitBounds = maximizeDigitBounds;
        if (maximizeDigitBounds)
            doMaximizeDigitBounds();
    }

    interface PathMaker {
        Path makePath();
    }

    abstract protected void addFontData();

    public void setMaximizeNumericBounds(boolean m) {
        if (maximizeDigitBounds == m)
            return;
        maximizeDigitBounds = m;
        map.clear();
        addFontData();
        if (maximizeDigitBounds) {
            doMaximizeDigitBounds();
        }
    }

    private void doMaximizeDigitBounds() {
        RectF bounds = null;

        for (char i='0' ; i <= '9' ; i++) {
            if (map.containsKey(i)) {
                RectF glyphBounds = map.get(i).bounds;
                if (bounds == null)
                    bounds = new RectF(glyphBounds);
                else
                    bounds.union(glyphBounds);
            }
        }

        if (bounds == null)
            return;

        for (char i='0' ; i <= '9' ; i++) {
            if (map.containsKey(i)) {
                map.get(i).bounds = bounds;
            }
        }
    }

    public void addCharacter(char c, float width, float lsb, PathMaker pm) {
        map.put(c, new Glyph(width, lsb, pm.makePath()));
    }

    protected void tweakWidth(char c, float newWidth) {
        Glyph g = map.get(c);

        if (g.width == newWidth)
            return;

        Matrix m = new Matrix();
        m.setTranslate((newWidth - g.width)/2f, 0);
        g.path.transform(m);

        g.width = newWidth;
    }

    public void defineFontSize(float s) {
        defaultFontSize = s;
    }

    public void getTextBounds(Paint paint, float letterSpacing, String text, int start, int end, RectF bounds) {
        float x = 0;
        if (end == start) {
            bounds.set(0, 0, 0, 0);
            return;
        }

        RectF glyphBounds = new RectF();

        for (int i=start; i<start+end; i++) {
            char c = text.charAt(i);
            try {
                Glyph g = map.get(c);
                glyphBounds.set(g.bounds);
                glyphBounds.left += x;
                glyphBounds.right += x;
                x += letterSpacing * g.width;
                if (i == start)
                    bounds.set(glyphBounds);
                else
                    bounds.union(glyphBounds);
            }
            catch(Exception e) {
            }
        }

        float scale = paint.getTextSize() / defaultFontSize;
        bounds.left *= scale * paint.getTextScaleX();
        bounds.right *= scale * paint.getTextScaleX();;
        bounds.bottom *= scale;
        bounds.top *= scale;
    }

    public void drawText(Canvas canvas, String text, float x, float y, Paint paint, float letterSpacing) {
        Matrix m = new Matrix();
        float scaleY = paint.getTextSize() / defaultFontSize;
        float scaleX = scaleY * paint.getTextScaleX();

        for (int i=0; i<text.length(); i++) {
            char c = text.charAt(i);
            try {
                Glyph g = map.get(c);
                Path p = new Path(g.path);
                m.setScale(scaleX, scaleY);
                m.postTranslate(x, y);
                p.transform(m);
                canvas.drawPath(p, paint);
                x += g.width * letterSpacing * scaleX;
            }
            catch(Exception e) {
            }
        }
    }

    static class Glyph {
        public final Path path;
        private float width;
        private final float lsb;
        RectF bounds;

        public Glyph(float width, float lsb, Path path) {
            this.width = width;
            this.lsb = lsb;
            this.path = path;
            bounds = new RectF();
            path.computeBounds(bounds, true);
        }
    }
}
