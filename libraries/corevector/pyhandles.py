from nodebox.handle import CombinedHandle, PointHandle, FourPointHandle, TranslateHandle, RotateHandle, ScaleHandle, CircleScaleHandle, FreehandHandle
from nodebox.graphics import Rect
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
        # Handles draw in screen space, so project the axis endpoints through the view transform.
        s1 = self.toScreen(x1, y1)
        s2 = self.toScreen(x2, y2)
        ctx.stroke(self.HANDLE_COLOR)
        ctx.line(s1.x, s1.y, s2.x, s2.y)
        CombinedHandle.draw(self, ctx)


class SnapHandle(PointHandle):

    def createHitRectangle(self, x, y):
        # The whole grid is the grab area: its document extent, projected to the screen.
        s0 = self.toScreen(-1000, -1000)
        s1 = self.toScreen(1000, 1000)
        return Rect(s0.x, s0.y, s1.x - s0.x, s1.y - s0.y)

    def draw(self, ctx):
        pos = self.getValue("position")
        snap_x = pos.x
        snap_y = pos.y
        distance = self.getValue("distance")
        ctx.stroke(0.4, 0.4, 0.4, 0.5)
        ctx.strokewidth(1.0)
        # Handles draw in screen space, so project the grid lines through the view transform.
        for i in xrange(-100, 100):
            x = -snap_x + (i * distance)
            y = -snap_y + (i * distance)
            v0 = self.toScreen(x, -1000)
            v1 = self.toScreen(x, 1000)
            h0 = self.toScreen(-1000, y)
            h1 = self.toScreen(1000, y)
            ctx.line(v0.x, v0.y, v1.x, v1.y)
            ctx.line(h0.x, h0.y, h1.x, h1.y)
