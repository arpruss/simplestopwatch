package omegacentauri.mobi.simplestopwatch;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.HashMap;
import java.util.Map;

public class MiniFont {
    public float defaultFontSize = 1;

    interface PathMaker {
        Path makePath();
    }

    public Map<Character, Glyph> map = new HashMap<Character, Glyph>();

    public void addCharacter(char c, float width, float lsb, PathMaker pm) {
        map.put(c, new Glyph(width, lsb, pm.makePath()));
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
        private final float width;
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
