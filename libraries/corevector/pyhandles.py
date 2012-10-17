from nodebox.handle import CombinedHandle, PointHandle, FourPointHandle, TranslateHandle, RotateHandle, ScaleHandle, CircleScaleHandle, FreehandHandle
from nodebox.util.Geometry import coordinates, angle, distance

class LineHandle(CombinedHandle):
    def __init__(self):
        CombinedHandle.__init__(self)
        self.addHandle(PointHandle("point1"))
        self.addHandle(PointHandle("point2"))
        self.update()
        

class StarHandle(CombinedHandle):
    def __init__(self):
        CombinedHandle.__init__(self)
        self.addHandle(PointHandle())
        self.addHandle(CircleScaleHandle("inner", CircleScaleHandle.Mode.DIAMETER, "position"))
        self.addHandle(CircleScaleHandle("outer", CircleScaleHandle.Mode.DIAMETER, "position"))
        self.update()
        
        
class PolygonHandle(CombinedHandle):
    def __init__(self):
        CombinedHandle.__init__(self)
        self.addHandle(PointHandle())
        self.addHandle(CircleScaleHandle("radius", CircleScaleHandle.Mode.RADIUS, "position"))
        self.update()


class ReflectHandle(CombinedHandle):
    def __init__(self):
        CombinedHandle.__init__(self)
        self.addHandle(TranslateHandle("position"))
        self.addHandle(RotateHandle("angle", "position"))
        self.update()

    def update(self):
        CombinedHandle.update(self)
        self.visible = self.isConnected("shape")

    def draw(self, ctx):
        pos = self.getValue("position")
        x = pos.x
        y = pos.y
        a = self.getValue("angle")
        x1, y1 = coordinates(x, y, -1000, a)
        x2, y2 = coordinates(x, y, 1000, a)
        ctx.stroke(self.HANDLE_COLOR)
        ctx.line(x1, y1, x2, y2)
        CombinedHandle.draw(self, ctx)


class SnapHandle(PointHandle):

    def createHitRectangle(self, x, y):
        return Rect(-1000, -1000, 2000, 2000)

    def draw(self, ctx):
        pos = self.getValue("position")
        snap_x = pos.x
        snap_y = pos.y
        distance = self.getValue("distance")
        ctx.stroke(0.4, 0.4, 0.4, 0.5)
        ctx.strokewidth(1.0)
        for i in xrange(-100, 100):
            x = -snap_x + (i * distance)
            y = -snap_y + (i * distance)
            ctx.line(x, -1000, x, 1000)
            ctx.line(-1000, y, 1000, y)
