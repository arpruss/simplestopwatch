package omegacentauri.mobi.simplestopwatch;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.HashMap;
import java.util.Map;

public class MiniFont {
    interface PathMaker {
        Path makePath();
    }

    public Map<Character, Glyph> map = new HashMap<Character, Glyph>();

    public void addCharacter(char c, float width, float lsb, PathMaker pm) {
        map.put(c, new Glyph(width, lsb, pm.makePath()));
    }

    public void measureText(String text, int start, int end, RectF bounds, float scale) {
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
                g.path.computeBounds(glyphBounds, false);
                glyphBounds.left *= scale;
                glyphBounds.right *= scale;
                glyphBounds.top *= scale;
                glyphBounds.bottom *= scale;
                glyphBounds.left += x;
                glyphBounds.right += x;
                x += g.width * scale;
                if (i == start)
                    bounds.set(glyphBounds);
                else
                    bounds.union(glyphBounds);
            }
            catch(Exception e) {
            }
        }
    }

    public void drawText(Canvas canvas, String text, int start, int end, RectF bounds, float scale, Paint paint) {
        float x = 0;
        if (end == start) {
            return;
        }

        for (int i=start; i<start+end; i++) {
            char c = text.charAt(i);
            try {
                Glyph g = map.get(c);
                Path p = new Path(g.path);
                Matrix m = new Matrix();
                m.setScale(scale, scale);
                m.postTranslate(x, 0f);
                p.transform(m);
                canvas.drawPath(p, paint);
                x += g.width * scale;
            }
            catch(Exception e) {
            }
        }
    }

    static class Glyph {
        public final Path path;
        private final float width;
        private final float lsb;

        public Glyph(float width, float lsb, Path path) {
            this.width = width;
            this.lsb = lsb;
            this.path = path;
        }
    }
}
