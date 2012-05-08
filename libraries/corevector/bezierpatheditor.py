from nodebox.graphics import Path, Point, Color, GraphicsContext
from nodebox.handle import AbstractHandle

from math import sin, cos, atan, pi, degrees, radians, sqrt, pow

import svg

MOVETO, LINETO, CURVETO, CLOSE = range(4)
CENTER = GraphicsContext.CENTER

class PathElement(object):
    def __init__(self, cmd=None, pts=None):
        self.cmd = cmd
        if cmd == MOVETO:
            assert len(pts) == 1
            self.x, self.y = pts[0]
            self.ctrl1 = Point(pts[0])
            self.ctrl2 = Point(pts[0])
        elif cmd == LINETO:
            assert len(pts) == 1
            self.x, self.y = pts[0]
            self.ctrl1 = Point(pts[0])
            self.ctrl2 = Point(pts[0])
        elif cmd == CURVETO:
            assert len(pts) == 3
            self.ctrl1 = Point(pts[0])
            self.ctrl2 = Point(pts[1])
            self.x, self.y = pts[2]
        elif cmd == CLOSE:
            assert pts is None or len(pts) == 0
            self.x = self.y = 0.0
            self.ctrl1 = Point(0.0, 0.0)
            self.ctrl2 = Point(0.0, 0.0)
        else:
            self.x = self.y = 0.0
            self.ctrl1 = Point()
            self.ctrl2 = Point()

    def __repr__(self):
        if self.cmd == MOVETO:
            return "PathElement(MOVETO, ((%.3f, %.3f),))" % (self.x, self.y)
        elif self.cmd == LINETO:
            return "PathElement(LINETO, ((%.3f, %.3f),))" % (self.x, self.y)
        elif self.cmd == CURVETO:
            return "PathElement(CURVETO, ((%.3f, %.3f), (%.3f, %s), (%.3f, %.3f))" % \
                (self.ctrl1.x, self.ctrl1.y, self.ctrl2.x, self.ctrl2.y, self.x, self.y)
        elif self.cmd == CLOSE:
            return "PathElement(CLOSE)"
           
    def __eq__(self, other):
        if other is None: return False
        if self.cmd != other.cmd: return False
        return self.x == other.x and self.y == other.y \
            and self.ctrl1 == other.ctrl1 and self.ctrl2 == other.ctrl2
       
    def __ne__(self, other):
        return not self.__eq__(other)

                                
class BezierPathEditor(AbstractHandle):
    def __init__(self):
        self.path = None
        self.path_string = ""
        self._points = []
        if self.getValue("path"):
            self.import_svg()
        self.reset()

    def reset(self):
        # These variables discern between different
        # modes of interaction.
        # In add-mode, new contains the last point added.
        # In edit-mode, edit contains the index of the point
        # in the path being edited.

        self.new = None
        self.edit = None
        self.editing = False
        self.insert = False
        self.inserting = False
        
        self.drag_point = False
        self.drag_handle1 = False
        self.drag_handle2 = False
        
        # Colors used to draw interface elements.
        
        self.strokewidth = 0.75
        self.path_color = Color(0.2, 0.2, 0.2)
        self.path_fill = Color(0, 0, 0, 0)
        self.handle_color = Color(0.6, 0.6, 0.6)
        self.new_color = Color(0.8, 0.8, 0.8)

        # Different states for button actions.
        # When delete contains a number,
        # delete that index from the path.
        # When moveto contains True,
        # do a MOVETO before adding the next new point.

        self.delete = None
        self.moveto = None
        self.last_moveto = None
        self.btn_r = 5
        self.btn_x = -5-1
        self.btn_y = -5*2

        # Keyboard keys pressed.
        
        self.keydown = False
        self._keycode = None
        self.last_key = None
        self.last_keycode = None
                
        self._mouseXY = None
        self.mousedown = False
        
    def overlap(self, x1, y1, x2, y2, r=5):
        
        """ Returns True when point 1 and point 2 overlap.
        
        There is an r treshold in which point 1 and point 2
        are considered to overlap.
        
        """
        
        if abs(x2-x1) < r and abs(y2-y1) < r:
            return True
        else:
            return False
    
    def reflect(self, x0, y0, x, y):
        
        """ Reflects the point x, y through origin x0, y0.
        """
                
        rx = x0 - (x-x0)
        ry = y0 - (y-y0)
        return rx, ry

    def angle(self, x0, y0, x1, y1):
        
        """ Calculates the angle between two points.
        """
    
        a = degrees( atan((y1-y0) / (x1-x0+0.00001)) ) + 360
        if x1-x0 < 0: a += 180
        return a

    def distance(self, x0, y0, x1, y1):
    
        """ Calculates the distance between two points.
        """
    
        return sqrt(pow(x1-x0, 2) + pow(y1-y0, 2))
        
    def coordinates(self, x0, y0, distance, angle):
        
        """ Calculates the coordinates of a point from the origin.
        """
        
        x = x0 + cos(radians(angle)) * distance
        y = y0 + sin(radians(angle)) * distance
        return Point(x, y)

    def contains_point(self, x, y, d=2):
        
        """ Returns true when x, y is on the path stroke outline.
        """
        if self.path != None and len(self._points) > 1 \
        and self.path.contains(x, y):
            # If all points around the mouse are also part of the path,
            # this means we are somewhere INSIDE the path.
            # Only points near the edge (i.e. on the outline stroke)
            # should propagate.
            if not self.path.contains(x+d, y) \
            or not self.path.contains(x, y+d) \
            or not self.path.contains(x-d, y) \
            or not self.path.contains(x, y-d) \
            or not self.path.contains(x+d, y+d) \
            or not self.path.contains(x-d, y-d) \
            or not self.path.contains(x+d, y-d) \
            or not self.path.contains(x-d, y+d):
                return True

        return False

    def insert_point(self, x, y):
        
        """ Inserts a point on the path at the mouse location.

        We first need to check if the mouse location is on the path.
        Inserting point is time intensive and experimental.
        
        """
        
        # TODO: implement
        pass 
        
    def _update_interaction(self):
        
        """ Update runs each frame to check for mouse interaction.
        
        Alters the path by allowing the user to add new points,
        drag point handles and move their location.
        Updates are automatically stored as SVG
        in the given filename.
        
        """
        
        x, y = self.mouse()
        
        if self.mousedown:
            # Handle buttons first.
            # When pressing down on a button, all other action halts.
            # Buttons appear near a point being edited.
            # Once clicked, actions are resolved.
            if self.edit != None \
            and not self.drag_point \
            and not self.drag_handle1 \
            and not self.drag_handle2:
                pt = self._points[self.edit]
                dx = pt.x+self.btn_x
                dy = pt.y+self.btn_y
                # The delete button
                if self.overlap(dx, dy, x, y, r=self.btn_r):
                    self.delete = self.edit
                    return
                # The moveto button,
                # active on the last point in the path.
                dx += self.btn_r*2 + 2
                if self.edit == len(self._points) -1 and \
                   self.overlap(dx, dy, x, y, r=self.btn_r):
                    self.moveto = self.edit
                    return
                    
            if self.insert:
                self.inserting = True
                return
            
            # When not dragging a point or the handle of a point,
            # i.e. the mousebutton was released and then pressed again,
            # check to see if a point on the path is pressed.
            # When this point is not the last new point,
            # enter edit mode.
            if not self.drag_point and \
               not self.drag_handle1 and \
               not self.drag_handle2:
                self.editing = False
                indices = range(len(self._points))
                indices.reverse()
                for i in indices:
                    pt = self._points[i]
                    if pt != self.new \
                    and self.overlap(x, y, pt.x, pt.y) \
                    and self.new == None:
                        # Don't select a point if in fact
                        # it is at the same location of the first handle 
                        # of the point we are currently editing.
                        if self.edit == i+1 \
                        and self.overlap(self._points[i+1].ctrl1.x,
                                         self._points[i+1].ctrl1.y, x, y):
                            continue
                        else:
                            self.edit = i
                            self.editing = True
                            break
            
            # When the mouse button is down,
            # edit mode continues as long as
            # a point or handle is dragged.
            # Else, stop editing and switch to add-mode
            # (the user is clicking somewhere on the canvas).
            if not self.editing:
                if self.edit != None:
                    pt = self._points[self.edit]
                    if self.overlap(pt.ctrl1.x, pt.ctrl1.y, x, y) or \
                       self.overlap(pt.ctrl2.x, pt.ctrl2.y, x, y):
                        self.editing = True
                    else:
                        self.edit = None
                    
            # When not in edit mode, there are two options.
            # Either no new point is defined and the user is
            # clicking somewhere on the canvas (add a new point)
            # or the user is dragging the handle of the new point.
            # Adding a new point is a fluid click-to-locate and
            # drag-to-curve action.
            if self.edit == None:
                if self.new == None:
                    # A special case is when the used clicked
                    # the moveto button on the last point in the path.
                    # This indicates a gap (i.e. MOVETO) in the path.
                    self.new = PathElement()
                    if self.moveto == True \
                    or len(self._points) == 0:
                        cmd = MOVETO
                        self.moveto = None
                        self.last_moveto = self.new
                    else:
                        cmd = CURVETO
                    self.new.cmd = cmd
                    self.new.x = x
                    self.new.y = y
                    self.new.ctrl1 = Point(x, y)
                    self.new.ctrl2 = Point(x, y)
                    # Don't forget to map the point's ctrl1 handle
                    # to the ctrl2 handle of the previous point.
                    # This makes for smooth, continuous paths.
                    if len(self._points) > 0:
                        prev = self._points[-1]
                        rx, ry = self.reflect(prev.x, prev.y, prev.ctrl2.x, prev.ctrl2.y)
                        self.new.ctrl1 = Point(rx, ry)
                    self._points.append(self.new)
                else:
                    # Illustrator-like behavior:
                    # when the handle is dragged downwards,
                    # the path bulges upwards.
                    rx, ry = self.reflect(self.new.x, self.new.y, x, y)
                    self.new.ctrl2 = Point(rx, ry)
            
            # Edit mode
            elif self.new == None:
            
                pt = self._points[self.edit]
            
                # The user is pressing the mouse on a point,
                # enter drag-point mode.
                if self.overlap(pt.x, pt.y, x, y) \
                and not self.drag_handle1 \
                and not self.drag_handle2 \
                and not self.new != None:
                    self.drag_point = True
                    self.drag_handle1 = False
                    self.drag_handle2 = False

                # The user is pressing the mouse on a point's handle,
                # enter drag-handle mode.
                if self.overlap(pt.ctrl1.x, pt.ctrl1.y, x, y) \
                and pt.cmd == CURVETO \
                and not self.drag_point \
                and not self.drag_handle2:
                    self.drag_point = False
                    self.drag_handle1 = True
                    self.drag_handle2 = False
                if self.overlap(pt.ctrl2.x, pt.ctrl2.y, x, y) \
                and pt.cmd == CURVETO \
                and not self.drag_point \
                and not self.drag_handle1:
                    self.drag_point = False
                    self.drag_handle1 = False
                    self.drag_handle2 = True
                
                # In drag-point mode,
                # the point is located at the mouse coordinates.
                # The handles move relatively to the new location
                # (e.g. they are retained, the path does not distort).
                # Modify the ctrl1 handle of the next point as well.
                if self.drag_point == True:
                    dx = x - pt.x
                    dy = y - pt.y
                    pt.x = x
                    pt.y = y
                    pt.ctrl2 = Point(pt.ctrl2.x + dx, pt.ctrl2.y + dy)
                    if self.edit < len(self._points)-1:
                        rx, ry = self.reflect(pt.x, pt.y, x, y)
                        next = self._points[self.edit+1]
                        next.ctrl1 = Point(next.ctrl1.x + dx, next.ctrl1.y + dy)

                # In drag-handle mode,
                # set the path's handle to the mouse location.
                # Rotate the handle of the next or previous point
                # to keep paths smooth - unless the user is pressing "x".
                if self.drag_handle1 == True:
                    pt.ctrl1 = Point(x, y)
                    if self.edit > 0 \
                    and self.last_keycode != 88:
                        prev = self._points[self.edit-1]
                        d = self.distance(prev.x, prev.y, prev.ctrl2.x, prev.ctrl2.y)
                        a = self.angle(prev.x, prev.y, pt.ctrl1.x, pt.ctrl1.y)
                        prev.ctrl2 = self.coordinates(prev.x, prev.y, d, a+180)                        
                if self.drag_handle2 == True:   
                    pt.ctrl2 = Point(x, y)
                    if self.edit < len(self._points)-1 \
                    and self.last_keycode != 88:
                        next = self._points[self.edit+1]
                        d = self.distance(pt.x, pt.y, next.ctrl1.x, next.ctrl1.y)
                        a = self.angle(pt.x, pt.y, pt.ctrl2.x, pt.ctrl2.y)
                        next.ctrl1 = self.coordinates(pt.x, pt.y, d, a+180)
        
        else:
            # The mouse button is released
            # so we are not dragging anything around.
            self.new = None
            self.drag_point = False
            self.drag_handle1 = False
            self.drag_handle2 = False
            
            # The delete button for a point was clicked.
            if self.delete != None and len(self._points) > 0:
                i = self.delete
                cmd = self._points[i].cmd
                del self._points[i]
                if 0 < i < len(self._points):
                    prev = self._points[i-1]
                    rx, ry = self.reflect(prev.x, prev.y, prev.ctrl2.x, prev.ctrl2.y)
                    self._points[i].ctrl1 = Point(rx, ry)
                start_i = i
                while i > 1:
                    i -= 1
                    pt = self._points[i]
                    if i < start_i-1:
                        if pt.cmd == MOVETO:
                            del self._points[i]
                        break
                # When you delete a MOVETO point,
                # the last moveto (the one where the dashed line points to)
                # needs to be updated.
                if len(self._points) > 0 \
                and (cmd == MOVETO or i == 0):
                    self.last_moveto = self._points[0]
                    for pt in self._points:
                        if pt.cmd == MOVETO:
                            self.last_moveto = pt
                self.delete = None
                self.edit = None

            # The moveto button for the last point
            # in the path was clicked.
            elif isinstance(self.moveto, int):
                self.moveto = True
                self.edit = None
            
            # We are not editing a node and
            # the mouse is hovering over the path outline stroke:
            # it is possible to insert a point here.
            elif self.edit == None \
            and self.contains_point(x, y, d=2):
                self.insert = True
            else:
                self.insert = False
            
            # Commit insert of new point.
            if self.inserting \
            and self.contains_point(x, y, d=2): 
                self.insert_point(x, y)
                self.insert = False
            self.inserting = False
        if self.keydown:    
            self.last_keycode = self._keycode
        if not self.keydown and self.last_keycode != None:
            self.last_key = None
            self.last_keycode = None

    def _update_path(self):
        x, y = self.mouse()
        path = Path()
        if len(self._points) > 0:
            first = True
            for i in range(len(self._points)):
                
                # Construct the path.
                pt = self._points[i]
                if first:
                    path.moveto(pt.x, pt.y)
                    first = False
                else:
                    if pt.cmd == CLOSE:
                        path.close()
                    elif pt.cmd == MOVETO:
                        path.moveto(pt.x, pt.y)
                    elif pt.cmd == LINETO:
                        path.lineto(pt.x, pt.y)
                    elif pt.cmd == CURVETO:
                        path.curveto(pt.ctrl1.x, pt.ctrl1.y, 
                                     pt.ctrl2.x, pt.ctrl2.y, 
                                     pt.x, pt.y)
        # Set the current path,
        self.path = path
        
    def update(self):
        if self.getValue("path") != self.path_string:
            self.import_svg()
            self.reset()
            
        # Enable interaction.
        self._update_interaction()
        self.export_svg()
        self._update_path()
        
    def draw(self, _ctx):
        
        """ Draws the editable path and interface elements.
        """

        x, y = self.mouse()
        
        _ctx.ellipsemode(CENTER)
        _ctx.rectmode(CENTER)
        _ctx.autoclosepath(False)
        _ctx.strokewidth(self.strokewidth)
        
        r = 4
        _ctx.nofill()
        if len(self._points) > 0:
            first = True
            for i in range(len(self._points)):
                
                # Construct the path.
                pt = self._points[i]
                
                # In add- or edit-mode,
                # display the current point's handles.
                if ((i == self.edit and self.new == None) \
                or pt == self.new) \
                and pt.cmd == CURVETO:
                    _ctx.stroke(self.handle_color)
                    _ctx.nofill()
                    _ctx.ellipse(pt.x, pt.y, r*2, r*2)
                    _ctx.stroke(self.handle_color)
                    _ctx.line(pt.ctrl2.x, pt.ctrl2.y, pt.x, pt.y)
                    _ctx.fill(self.handle_color)
                # Display the new point's handle being dragged.
                if pt == self.new:
                    rx, ry = self.reflect(pt.x, pt.y, pt.ctrl2.x, pt.ctrl2.y)
                    _ctx.stroke(self.handle_color)
                    _ctx.line(rx, ry, pt.x, pt.y)
                    _ctx.nostroke()
                    _ctx.fill(self.handle_color)
                    _ctx.ellipse(rx, ry, r, r)
                # Display handles for point being edited.
                if i == self.edit \
                and self.new == None \
                and pt.cmd == CURVETO:
                    _ctx.ellipse(pt.ctrl2.x, pt.ctrl2.y, r, r)
                    if i > 0:
                        prev = self._points[i-1]
                        _ctx.line(pt.ctrl1.x, pt.ctrl1.y, prev.x, prev.y)
                        _ctx.ellipse(pt.ctrl1.x, pt.ctrl1.y, r, r)
                    if i > 0 and self._points[i-1].cmd != MOVETO:
                        _ctx.line(prev.ctrl2.x, prev.ctrl2.y, prev.x, prev.y)
                    if i < len(self._points)-1:
                        next = self._points[i+1]
                        if next.cmd == CURVETO:
                            _ctx.line(next.ctrl1.x, next.ctrl1.y, pt.x, pt.y)
                
                # When hovering over a point,
                # highlight it.
                elif self.overlap(x, y, pt.x, pt.y):
                    self.insert = False # quit insert mode
                    _ctx.nofill()
                    _ctx.stroke(self.handle_color)
                    _ctx.ellipse(pt.x, pt.y, r*2, r*2)
                
                if pt.cmd != MOVETO:
                    _ctx.fill(self.path_color)
                    _ctx.nostroke()
                else:
                    _ctx.stroke(self.path_color)
                    _ctx.nofill()
                _ctx.ellipse(pt.x, pt.y, r, r)
                

            # Possible to insert a point here.
            if self.insert:
                _ctx.stroke(self.handle_color)
                _ctx.nofill()
                _ctx.ellipse(x, y, r*1.6, r*1.6)
                
            # When not editing a node,
            # prospect how the curve will continue
            # when adding a new point.
            if self.edit == None \
            and self.new == None \
            and self.moveto != True:
                _ctx.nofill()
                _ctx.stroke(self.new_color)
                rx, ry = self.reflect(pt.x, pt.y, pt.ctrl2.x, pt.ctrl2.y)
                _ctx.beginpath(pt.x, pt.y)
                _ctx.curveto(rx, ry, x, y, x, y)
                _ctx.endpath()

                # A dashed line indicates what
                # a CLOSETO would look like.
                if self.last_moveto != None:
                    start = self.last_moveto
                else:
                    start = self._points[0]
        
            # When doing a MOVETO,
            # show the new point hovering at the mouse location.
            elif self.edit == None \
            and self.new == None \
            and self.moveto != None:
                _ctx.stroke(self.new_color)
                _ctx.nofill()
                _ctx.ellipse(x, y, r*1.6, r*1.6)
            
            # Draws button for a point being edited.
            # The first button deletes the point.
            # The second button, which appears only on the last point
            # in the path, tells the editor to perform a MOVETO
            # before adding a new point.
            if self.edit != None:
                pt = self._points[self.edit]
                x = pt.x + self.btn_x
                y = pt.y + self.btn_y
                r = self.btn_r
                _ctx.nostroke()
                _ctx.fill(self.handle_color)
                _ctx.ellipse(x, y, r*2, r*2)
                _ctx.fill(1)
                _ctx.rotate(45)
                _ctx.rect(x, y, r+1, 1.25)
                _ctx.rotate(-90)
                _ctx.rect(x, y, r+1, 1.25)
                _ctx.reset()
                if self.edit == len(self._points)-1:
                    _ctx.fill(self.handle_color)
                    _ctx.ellipse(x+r*2+2, y, r*2, r*2)
                    _ctx.fill(1)
                    _ctx.rect(x+r*2+3-2.25, y, 1.5, r-1)
                    _ctx.rect(x+r*2+3+0.75, y, 1.5, r-1)
        
    def import_svg(self):
        # Parses the string from the node's path parameter and constructs a path
        self.path_string = self.getValue("path")
        self.path = svg.path_from_string(self.path_string)
        self._points = []
        
        # Converts the path data into a list of PathElements
        for contour in self.path.contours:
            if not (len(contour.points) == 1 and contour.points[0].x == 0 and contour.points[0].y == 0):
                first = None
                curvePoint = PathElement()
                curvePoint.cmd = CURVETO
                first_curvepoint = True
                for point in contour.points:
                    if first is None or point.isLineTo():
                        # Checks for MOVETO or LINETO PathElements.
                        # The first point of a Curve is always a MOVETO,
                        # The other ones are LINETO's.
                        pe = PathElement()
                        if first is None:
                            first = point
                            pe.cmd = MOVETO
                        else:
                            pe.cmd = LINETO
                        pe.x = point.x
                        pe.y = point.y
                        self._points.append(pe)
                    else:
                        # Checks for CURVETO PathElements.
                        # A new element will only be appended to the points list
                        # when all necessary data has been found by going through the loop
                        if first_curvepoint:
                            curvePoint.ctrl1 = point
                            first_curvepoint = False
                        elif point.isOffCurve():
                            curvePoint.ctrl2 = point
                        else:
                            curvePoint.x = point.x
                            curvePoint.y = point.y
                            self._points.append(curvePoint)
                            curvePoint = PathElement()
                            curvePoint.cmd = CURVETO
                            first_curvepoint = True
                if contour.closed:
                    pe = PathElement()
                    pe.cmd = CLOSE  
                    self._points.append(pe)    

    def export_svg(self):
        # Converts all the point data into SVG format.
        s = ""
        if len(self._points) > 0:
            for pt in self._points:
                if pt.cmd == MOVETO:
                    s += "M " + str(pt.x) + " " + str(pt.y) + " "
                elif pt.cmd == LINETO:
                    s += "L " + str(pt.x) + " " + str(pt.y) + " "
                elif pt.cmd == CURVETO:
                    s += "C "
                    s += str(pt.ctrl1.x) + " " + str(pt.ctrl1.y) + " "
                    s += str(pt.ctrl2.x) + " " + str(pt.ctrl2.y) + " "
                    s += str(pt.x) + " " + str(pt.y) + " "
            self.path_string = s
            if self.path_string != self.getValue("path"):
                self.silentSet("path", s)
                self.updateHandle()
    
    def mouse(self):
        if self._mouseXY is not None:
            return (self._mouseXY.x, self._mouseXY.y)
        else:
            return (0, 0)

    def mousePressed(self, pt):
        self._mouseXY = pt
        self.mousedown = True
        self.updateHandle()
        return True
        
    def mouseDragged(self, pt):
        if not self.mousedown: return False
        self._mouseXY = pt
        self.updateHandle()
        return True

    def mouseReleased(self, pt):
        if not self.mousedown: return False
        self._mouseXY = pt
        self.mousedown = False
        self.updateHandle()
        self.stopCombiningEdits()
        return True
    
    def mouseMoved(self, pt):
        self._mouseXY = pt
        self.updateHandle()
        return True

    def keyPressed(self, keycode, modifiers):
        self._keycode = keycode
        self.keydown = True
        self.updateHandle()
        return True

    def keyReleased(self, keycode, modifiers):
        if not self.keydown: return False
        self._keycode = keycode
        self.keydown = False
        self.updateHandle()
        return True
