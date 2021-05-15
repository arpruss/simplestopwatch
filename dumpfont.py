from fontTools.ttLib import TTFont
from fontTools.pens.basePen import decomposeQuadraticSegment
from sys import argv

charsToDump = "0123456789.:\u2212"
charsToNarrow = ".:"
narrowFraction = 0.8
maximizeDigitBounds = True
equalizeWidths = "0123456789"
fontName = "Roboto-Regular.ttf" if len(argv) < 2 else argv[1]
className = "SansDigitsColon" if len(argv) < 3 else argv[2]

ttf = TTFont(fontName)
glyphs = ttf.getGlyphSet()
map = ttf.getBestCmap()


def multiply(m1,m2):
    return tuple( tuple(sum(m1[i][j]*m2[j][k] for j in range(len(m1[0]))) for k in range(len(m2[0]))) for i in range(len(m1)) )
    
def apply2d(m1,v):
    if v is None:
        return None
    else:
        return ( m1[0][0]*v[0] + m1[0][1]*v[1] + m1[0][2], m1[1][0]*v[0] + m1[1][1]*v[1] + m1[1][2] )

class MyPen(object):
    def __init__(self, indent="", transformation=[[1,0,0],[0,-1,0],[0,0,1]]):
        self.transformation = transformation
        self.indent = indent
    def shiftList(self, points):
        return [apply2d(self.transformation,point) for point in points]
    def shift(self, point):
        return apply2d(self.transformation,point)
    def moveTo(self, point):
        print(self.indent+"path.moveTo(%gf,%gf);" % self.shift(point))
#    def curveTo(self, *points):
#        print(self.indent+"curveTo", self.shift(*points))
    def qCurveTo(self, *points):
        if points and points[-1] is None:
            // this shouldn't happen but it does
            print(self.indent+"path.moveTo(%gf,%gf);" % self.shift(points[0]))        
            points = points[:-1]
        decomp = decomposeQuadraticSegment(points)
        for pair in decomp:
            shifted = self.shiftList(pair)
            if shifted[0] and shifted[1]:
                print(self.indent+"path.quadTo(%gf,%gf,%gf,%gf);" % tuple(shifted[0]+shifted[1]))
    def lineTo(self, point):
        print(self.indent+"path.lineTo(%gf,%gf);" % self.shift(point))
    def closePath(self):
        print(self.indent+"path.close();")
    def endPath(self):
        print(self.indent+"// endPath")
    def addComponent(self, glyphName, t):
        print(self.indent+"// addComponent", glyphName, t)
        glyphs[glyphName].draw(MyPen(indent=self.indent,transformation=multiply(self.transformation,[ [t[0],t[2],t[4]],[t[1],t[3],t[5]],[0,0,1] ])))

class PointListPen(object):
    def __init__(self, transformation=[[1,0,0],[0,-1,0],[0,0,1]], pointList=[]):
        self.transformation = transformation
        self.pointList = pointList
    def update(self, point):
        p = apply2d(self.transformation,point)
        self.pointList.append(p)
    def moveTo(self, point):
        self.update(point)
#    def curveTo(self, *points):
#        print(self.indent+"curveTo", self.shift(*points))
    def qCurveTo(self, *points):
        decomp = decomposeQuadraticSegment(points)
        for pair in decomp:
            self.update(pair[1])
    def lineTo(self, point):
        self.update(point)
    def closePath(self):
        pass
    def endPath(self):
        pass
    def addComponent(self, glyphName, t):
        glyphs[glyphName].draw(PointListPen(pointList=self.pointList, transformation=multiply(self.transformation,[ [t[0],t[2],t[4]],[t[1],t[3],t[5]],[0,0,1] ])))
        
plPen = PointListPen()
glyphs["M"].draw(plPen)
glyphs["y"].draw(plPen)
minY = min(p[1] for p in plPen.pointList)
maxY = max(p[1] for p in plPen.pointList)

print("""package omegacentauri.mobi.simplestopwatch;

import android.graphics.Path;

public class %s extends MiniFont {
  public %s() {
    super(%s);
  }
  
  public void addFontData() {
    defineFontSize(%gf);
""" % (className, className, "true" if maximizeDigitBounds else "false", maxY-minY))

def getGlyph(c):
    try:
        return glyphs[map[ord(c)]]
    except:
        if c == '\u2212':
            return glyphs[map[ord('-')]]
        else:
            print("Error fetching glyph",c)
            raise KeyError()

for c in charsToDump:
    glyph = getGlyph(c)
    
    print("  addCharacter((char)%d,%gf,%gf,new PathMaker() {" % (ord(c),glyph.width,glyph.lsb))
    print("    @Override")
    print("    public Path makePath() {")
    print("      Path path = new Path();")
    
    glyph.draw(MyPen(indent="      ", transformation=[[1,0,0],[0,-1,0],[0,0,1]]))
    
    print("      return path;")
    print("      }")
    print("    });")

    
for c in charsToNarrow:
    print("  tweakWidth((char)%d,%gf);" % (ord(c),getGlyph(c).width*narrowFraction))
    
if equalizeWidths:
    maxW = max(getGlyph(c).width for c in equalizeWidths)
    for c in equalizeWidths:
        if getGlyph(c).width != maxW:
            print("  tweakWidth((char)%d,%gf);" % (ord(c),maxW))
    
print("  }")
print("}")
