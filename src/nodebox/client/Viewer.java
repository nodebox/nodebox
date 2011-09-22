package nodebox.client;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PPaintContext;
import nodebox.graphics.Canvas;
import nodebox.graphics.*;
import nodebox.handle.Handle;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

public class Viewer extends PCanvas implements PaneView, MouseListener, MouseMotionListener, KeyListener {

    public static final float POINT_SIZE = 4f;

    public static final float MIN_ZOOM = 0.1f;
    public static final float MAX_ZOOM = 16.0f;

    private static final String HANDLE_UNDO_TEXT = "Handle Changes";
    private static final String HANDLE_UNDO_TYPE = "handle";

    private static Cursor defaultCursor, panCursor;

    private final NodeBoxDocument document;
    private Object outputValue;
    private Handle handle;
    private boolean showHandle = true;
    private boolean handleEnabled = true;
    private boolean showPoints = false;
    private boolean showPointNumbers = false;
    private boolean showOrigin = false;
    private boolean panEnabled = false;
    private PLayer viewerLayer;
    private JPopupMenu viewerMenu;
    private Class outputClass;
    private ViewerEventListener listener;

    static {
        Image panCursorImage;

        try {
            if (PlatformUtils.onWindows())
                panCursorImage = ImageIO.read(new File("res/view-cursor-pan-32.png"));
            else
                panCursorImage = ImageIO.read(new File("res/view-cursor-pan.png"));
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            panCursor = toolkit.createCustomCursor(panCursorImage, new Point(0, 0), "PanCursor");
            defaultCursor = Cursor.getDefaultCursor();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Viewer(final NodeBoxDocument document) {
        this.document = document;
        addMouseListener(this);
        addMouseMotionListener(this);
        setFocusable(true);
        addKeyListener(this);
        // Setup Piccolo canvas
        setBackground(Theme.VIEWER_BACKGROUND_COLOR);
        setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        // Remove default panning and zooming behaviour
        removeInputEventListener(getPanEventHandler());
        removeInputEventListener(getZoomEventHandler());
        // Install custom panning and zooming
        PInputEventFilter panFilter = new PInputEventFilter();
        panFilter.setNotMask(InputEvent.CTRL_MASK);
        PPanEventHandler panHandler = new PPanEventHandler() {
            public void processEvent(final PInputEvent evt, final int i) {
                if (evt.isMouseEvent() && evt.isLeftMouseButton() && panEnabled)
                    super.processEvent(evt, i);
            }
        };
        panHandler.setAutopan(false);
        panHandler.setEventFilter(panFilter);
        addInputEventListener(panHandler);
        setZoomEventHandler(new PZoomEventHandler() {
            public void processEvent(final PInputEvent evt, final int i) {
                if (evt.isMouseWheelEvent()) {
                    double currentScale = evt.getCamera().getViewScale();
                    double scaleDelta = 1D - 0.1 * evt.getWheelRotation();
                    double newScale = currentScale * scaleDelta;
                    if (newScale < MIN_ZOOM) {
                        scaleDelta = MIN_ZOOM / currentScale;
                    } else if (newScale > MAX_ZOOM) {
                        scaleDelta = MAX_ZOOM / currentScale;
                    }
                    final Point2D p = evt.getPosition();
                    evt.getCamera().scaleViewAboutPoint(scaleDelta, p.getX(), p.getY());
                }
            }
        });
        // Add the zoomable view layer
        viewerLayer = new ViewerLayer();
        getCamera().addLayer(0, viewerLayer);

        initMenus();
    }

    public void setEventListener(ViewerEventListener e) {
        listener = e;
    }

    private void initMenus() {
        viewerMenu = new JPopupMenu();
        viewerMenu.add(new ResetViewAction());
        PopupHandler popupHandler = new PopupHandler();
        addInputEventListener(popupHandler);
    }

    public boolean isShowHandle() {
        return showHandle;
    }

    public void setShowHandle(boolean showHandle) {
        this.showHandle = showHandle;
        repaint();
    }

    public boolean isShowPoints() {
        return showPoints;
    }

    public void setShowPoints(boolean showPoints) {
        this.showPoints = showPoints;
        repaint();
    }

    public boolean isShowPointNumbers() {
        return showPointNumbers;
    }

    public void setShowPointNumbers(boolean showPointNumbers) {
        this.showPointNumbers = showPointNumbers;
        repaint();
    }

    public boolean isShowOrigin() {
        return showOrigin;
    }

    public void setShowOrigin(boolean showOrigin) {
        this.showOrigin = showOrigin;
        repaint();
    }
    
    //// Handle support ////

    public Handle getHandle() {
        return handle;
    }

    public void setHandle(Handle handle) {
        this.handle = handle;
        repaint();
    }

    public boolean hasVisibleHandle() {
        if (handle == null) return false;
        if (!showHandle) return false;
        if (!handleEnabled) return false;
        return handle.isVisible();
    }

    //// Network data events ////


    public Object getOutputValue() {
        return outputValue;
    }

    public void setOutputValue(Object outputValue) {
        this.outputValue = outputValue;
        if (outputValue != null && outputClass != outputValue.getClass()) {
            if (outputValue instanceof Canvas) {
                // The canvas is placed in the top-left corner, as in NodeBox 1.
                resetView();
                Canvas canvas = (Canvas) outputValue;
                viewerLayer.setBounds(canvas.getBounds().getRectangle2D());
                viewerLayer.setOffset(getWidth() / 2, getHeight() / 2);
            } else if (outputValue instanceof Grob) {
                // Other graphic objects are displayed in the center.
                resetView();
                viewerLayer.setBounds(-Integer.MAX_VALUE / 2, -Integer.MAX_VALUE / 2, Integer.MAX_VALUE, Integer.MAX_VALUE);
                viewerLayer.setOffset(getWidth() / 2, getHeight() / 2);
            } else {
                // Other output will be converted to a string, and placed just off the top-left corner.
                resetView();
                viewerLayer.setBounds(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
                viewerLayer.setOffset(5, 5);
            }
            outputClass = outputValue.getClass();
        }
        repaint();
    }

    //// Node attribute listener ////


    public boolean isHandleEnabled() {
        return handleEnabled;
    }

    public void setHandleEnabled(boolean handleEnabled) {
        this.handleEnabled = handleEnabled;
    }

    public void reloadHandle() {
        // TODO Implement
    }

    public void resetView() {
        getCamera().setViewTransform(new AffineTransform());
    }

    //// Mouse events ////

    private nodebox.graphics.Point pointForEvent(MouseEvent e) {
        Point2D originalPoint = new Point2D.Float(e.getX(), e.getY());
        PAffineTransform transform = getCamera().getViewTransform();
        Point2D transformedPoint;
        try {
            transformedPoint = transform.inverseTransform(originalPoint, null);
        } catch (NoninvertibleTransformException ex) {
            return new nodebox.graphics.Point(0, 0);
        }
        Point2D offset = viewerLayer.getOffset();
        double cx = -offset.getX() + transformedPoint.getX();
        double cy = -offset.getY() + transformedPoint.getY();
//        double cx = -getWidth() / 2.0 + transformedPoint.getX();
//        double cy = -getHeight() / 2.0 + transformedPoint.getY();
        return new nodebox.graphics.Point((float) cx, (float) cy);
    }

    public void mouseClicked(MouseEvent e) {
        // We register the mouse click as an edit since it can trigger a change to the node.
        if (e.isPopupTrigger()) return;
        if (hasVisibleHandle()) {
            //getDocument().addEdit(HANDLE_UNDO_TEXT, HANDLE_UNDO_TYPE, activeNode);
            handle.mouseClicked(pointForEvent(e));
        }
    }

    public void mousePressed(MouseEvent e) {
        // We register the mouse press as an edit since it can trigger a change to the node.
        if (e.isPopupTrigger()) return;
        if (hasVisibleHandle()) {
            //getDocument().addEdit(HANDLE_UNDO_TEXT, HANDLE_UNDO_TYPE, activeNode);
            handle.mousePressed(pointForEvent(e));
        }
    }

    public void mouseReleased(MouseEvent e) {
        // We register the mouse release as an edit since it can trigger a change to the node.
        if (e.isPopupTrigger()) return;
        if (hasVisibleHandle()) {
            //getDocument().addEdit(HANDLE_UNDO_TEXT, HANDLE_UNDO_TYPE, activeNode);
            handle.mouseReleased(pointForEvent(e));
        }
    }

    public void mouseEntered(MouseEvent e) {
        // Entering the viewer with your mouse should not change the node, so we do not register an edit.
        if (e.isPopupTrigger()) return;
        if (hasVisibleHandle()) {
            handle.mouseEntered(pointForEvent(e));
        }
    }

    public void mouseExited(MouseEvent e) {
        // Exiting the viewer with your mouse should not change the node, so we do not register an edit.
        if (e.isPopupTrigger()) return;
        if (hasVisibleHandle()) {
            handle.mouseExited(pointForEvent(e));
        }
    }

    public void mouseDragged(MouseEvent e) {
        // We register the mouse drag as an edit since it can trigger a change to the node.
        if (e.isPopupTrigger()) return;
        if (hasVisibleHandle()) {
            //getDocument().addEdit(HANDLE_UNDO_TEXT, HANDLE_UNDO_TYPE, activeNode);
            handle.mouseDragged(pointForEvent(e));
        }
    }

    public void mouseMoved(MouseEvent e) {
        // Moving the mouse in the viewer area should not change the node, so we do not register an edit.
        if (e.isPopupTrigger()) return;
        if (hasVisibleHandle()) {
            handle.mouseMoved(pointForEvent(e));
        }
    }

    public void keyTyped(KeyEvent e) {
        if (hasVisibleHandle())
            handle.keyTyped(e.getKeyCode(), e.getModifiersEx());
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            panEnabled = true;
             if (! getCursor().equals(panCursor))
                setCursor(panCursor);
        }
        if (hasVisibleHandle())
            handle.keyPressed(e.getKeyCode(), e.getModifiersEx());
    }

    public void keyReleased(KeyEvent e) {
        panEnabled = false;
        if (! getCursor().equals(defaultCursor))
            setCursor(defaultCursor);
        if (hasVisibleHandle())
            handle.keyReleased(e.getKeyCode(), e.getModifiersEx());
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw the origin.
        Point2D origin = getCamera().getViewTransform().transform(viewerLayer.getOffset(), null);
        int x = (int) Math.round(origin.getX());
        int y = (int) Math.round(origin.getY());
        if (showOrigin) {
            g.setColor(Color.DARK_GRAY);
            g.drawLine(x, 0, x, getHeight());
            g.drawLine(0, y, getWidth(), y);
        }
    }

    public class ViewerLayer extends PLayer {

        @Override
        protected void paint(PPaintContext paintContext) {
            Graphics2D g2 = paintContext.getGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // If the output value is geometry, store it in a geometry object.
            // This is used to draw the points / point numbers.
            IGeometry geo = null;
            // Draw the canvas bounds
            if (outputValue instanceof Canvas) {
                Canvas c = (Canvas) outputValue;
                geo = c.asGeometry(false);
                Rectangle2D canvasBounds = c.getBounds().getRectangle2D();
                g2.setColor(Color.DARK_GRAY);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(canvasBounds);
            }
            if (outputValue instanceof Grob) {
                Grob g = (Grob) outputValue;
                if (g instanceof IGeometry) {
                    geo = (IGeometry) outputValue;
                }
                Shape oldClip = g2.getClip();
                g.draw(g2);
                g2.setClip(oldClip);
            } else if (outputValue != null) {
                g2.setColor(Theme.TEXT_NORMAL_COLOR);
                g2.setFont(Theme.EDITOR_FONT);
                String s = outputValue.toString();
                int y = 20;
                for (String line : s.split("\n")) {
                    g2.drawString(line, 5, y);
                    y += 14;
                }
            }

            // Draw the handle.
            if (hasVisibleHandle()) {
                // Create a canvas with a transparent background
                nodebox.graphics.Canvas canvas = new nodebox.graphics.Canvas();
                canvas.setBackground(new nodebox.graphics.Color(0, 0, 0, 0));
                CanvasContext ctx = new CanvasContext(canvas);
                handle.draw(ctx);
                ctx.getCanvas().draw(g2);
            }

            // Draw the points.
            if (showPoints && geo != null) {
                // Create a canvas with a transparent background
                Path onCurves = new Path();
                Path offCurves = new Path();
                onCurves.setFill(new nodebox.graphics.Color(0f, 0f, 1f));
                offCurves.setFill(new nodebox.graphics.Color(1f, 0f, 0f));
                for (nodebox.graphics.Point pt : geo.getPoints()) {
                    if (pt.isOnCurve()) {
                        onCurves.ellipse(pt.x, pt.y, POINT_SIZE, POINT_SIZE);
                    } else {
                        offCurves.ellipse(pt.x, pt.y, POINT_SIZE, POINT_SIZE);
                    }
                }
                onCurves.draw(g2);
                offCurves.draw(g2);
            }

            // Draw the point numbers.
            if (showPointNumbers && geo != null) {
                g2.setFont(Theme.SMALL_MONO_FONT);
                g2.setColor(Color.BLUE);
                // Create a canvas with a transparent background
                int index = 0;
                for (nodebox.graphics.Point pt : geo.getPoints()) {
                    if (pt.isOnCurve()) {
                        g2.setColor(Color.BLUE);
                    } else {
                        g2.setColor(Color.RED);
                    }
                    g2.drawString(index + "", pt.x + 3, pt.y - 2);
                    index++;
                }
            }
        }
    }

    private class PopupHandler extends PBasicInputEventHandler {
        public void processEvent(PInputEvent e, int i) {
            if (!e.isPopupTrigger()) return;
            if (e.isHandled()) return;
            Point2D p = e.getCanvasPosition();
            viewerMenu.show(Viewer.this, (int) p.getX(), (int) p.getY());
        }
    }


    private class ResetViewAction extends AbstractAction {
        private ResetViewAction() {
            super("Reset View");
        }

        public void actionPerformed(ActionEvent e) {
            resetView();
        }
    }

}
