package nodebox.client;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PPaintContext;
import nodebox.client.visualizer.*;
import nodebox.graphics.CanvasContext;
import nodebox.graphics.IGeometry;
import nodebox.handle.Handle;
import nodebox.ui.Platform;
import nodebox.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkNotNull;
import static nodebox.util.ListUtils.listClass;

public class Viewer extends PCanvas implements OutputView, MouseListener, MouseMotionListener, KeyListener {

    public static final float MIN_ZOOM = 0.01f;
    public static final float MAX_ZOOM = 64.0f;

    private static final ImmutableList<Visualizer> visualizers;
    private static final Visualizer DEFAULT_VISUALIZER = LastResortVisualizer.INSTANCE;

    private static final Cursor defaultCursor, panCursor;

    private final NodeBoxDocument document;
    private final PLayer viewerLayer;
    private final JPopupMenu viewerMenu;

    private Handle handle;
    private boolean showHandle = true;
    private boolean handleEnabled = true;
    private boolean showPoints = false;
    private boolean showPointNumbers = false;
    private boolean showOrigin = false;
    private boolean panEnabled = false;

    private java.util.List<Object> outputValues;
    private Class valuesClass;
    private Visualizer currentVisualizer;

    static {
        Image panCursorImage;
        try {
            if (Platform.onWindows())
                panCursorImage = ImageIO.read(new File("res/view-cursor-pan-32.png"));
            else
                panCursorImage = ImageIO.read(new File("res/view-cursor-pan.png"));
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            panCursor = toolkit.createCustomCursor(panCursorImage, new Point(0, 0), "PanCursor");
            defaultCursor = Cursor.getDefaultCursor();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        visualizers = ImmutableList.of(CanvasVisualizer.INSTANCE, GrobVisualizer.INSTANCE, PointVisualizer.INSTANCE, ColorVisualizer.INSTANCE);
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
                    double scaleDelta = 1D - 0.05 * evt.getWheelRotation();
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

    public void updateHandle() {
        if (handle == null) return;
        handle.update();
    }

    public boolean hasVisibleHandle() {
        if (handle == null) return false;
        if (!showHandle) return false;
        if (!handleEnabled) return false;

        // Don't show handles when the window offset of LastResortVisualizer and ColorVisualizer
        // doesn't match the visualizer for Grob, Canvas or Point, resulting in the handle showing up at
        // a weird location.
        // Todo: Find a better solution, because this feels a bit hacky.
        if (currentVisualizer instanceof LastResortVisualizer) return false;
        if (currentVisualizer instanceof ColorVisualizer) return false;

        return handle.isVisible();
    }

    //// Network data events ////


    public java.util.List getOutputValues() {
        return outputValues;
    }

    public void setOutputValues(java.util.List<Object> outputValues) {
        this.outputValues = outputValues;
        valuesClass = listClass(outputValues);
        Visualizer visualizer = getVisualizer(outputValues, valuesClass);
        if (visualizer instanceof LastResortVisualizer && outputValues.size() == 0) {
            // This scenario means likely that we're in a node that normally outputs
            // some visual type but currently outputs null (or None)
            // If we'd reset the visualizer the screen offset would change, and this would
            // lead to strange (and wrong) interactions with handles (big leaps in
            // current mouse locations).
            repaint();
            return;
        }
        if (currentVisualizer != visualizer) {
            resetView();
            currentVisualizer = visualizer;
            viewerLayer.setBounds(visualizer.getBounds(outputValues));
            viewerLayer.setOffset(visualizer.getOffset(outputValues, getSize()));
        }
        checkNotNull(currentVisualizer);
        repaint();
    }

    public static Visualizer getVisualizer(Iterable<?> objects, Class listClass) {
        for (Visualizer visualizer : visualizers) {
            if (visualizer.accepts(objects, listClass))
                return visualizer;
        }
        return DEFAULT_VISUALIZER;
    }

    //// Node attribute listener ////


    public boolean isHandleEnabled() {
        return handleEnabled;
    }

    public void setHandleEnabled(boolean handleEnabled) {
        if (this.handleEnabled != handleEnabled) {
            this.handleEnabled = handleEnabled;
            // We could just repaint the handle.
            repaint();
        }
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
        transformedPoint = transform.inverseTransform(originalPoint, null);
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
            if (!getCursor().equals(panCursor))
                setCursor(panCursor);
        }
        if (hasVisibleHandle())
            handle.keyPressed(e.getKeyCode(), e.getModifiersEx());
    }

    public void keyReleased(KeyEvent e) {
        panEnabled = false;
        if (!getCursor().equals(defaultCursor))
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
            Graphics2D g = paintContext.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawObjects(g);
            drawHandle(g);
            drawPoints(g);
            drawPointNumbers(g);
        }

        public void drawObjects(Graphics2D g) {
            if (currentVisualizer != null)
                currentVisualizer.draw(g, outputValues);
        }

        public void drawHandle(Graphics2D g) {
            if (hasVisibleHandle()) {
                // Create a canvas with a transparent background.
                nodebox.graphics.Canvas canvas = new nodebox.graphics.Canvas();
                canvas.setBackground(new nodebox.graphics.Color(0, 0, 0, 0));
                CanvasContext ctx = new CanvasContext(canvas);
                try {
                    handle.draw(ctx);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ctx.getCanvas().draw(g);
            }
        }

        private void drawPoints(Graphics2D g) {
            if (showPoints && IGeometry.class.isAssignableFrom(valuesClass)) {
                // TODO Create a dynamic iterator that combines all output values into one flat sequence.
                LinkedList<nodebox.graphics.Point> points = new LinkedList<nodebox.graphics.Point>();
                for (Object o : outputValues) {
                    IGeometry geo = (IGeometry) o;
                    points.addAll(geo.getPoints());
                }
                PointVisualizer.drawPoints(g, points);
            }
        }

        private void drawPointNumbers(Graphics2D g) {
            if (showPointNumbers && IGeometry.class.isAssignableFrom(valuesClass)) {
                g.setFont(Theme.SMALL_MONO_FONT);
                g.setColor(Color.BLUE);
                // Create a canvas with a transparent background.
                int index = 0;
                for (Object o : outputValues) {
                    IGeometry geo = (IGeometry) o;
                    for (nodebox.graphics.Point pt : geo.getPoints()) {
                        if (pt.isOnCurve()) {
                            g.setColor(Color.BLUE);
                        } else {
                            g.setColor(Color.RED);
                        }
                        g.drawString(index + "", (int) (pt.x + 3), (int) (pt.y - 2));
                        index++;
                    }
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
