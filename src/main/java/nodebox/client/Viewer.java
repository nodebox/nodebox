package nodebox.client;

import com.google.common.collect.ImmutableList;
import nodebox.client.visualizer.*;
import nodebox.graphics.CanvasContext;
import nodebox.graphics.IGeometry;
import nodebox.handle.Handle;
import nodebox.ui.Platform;
import nodebox.ui.Theme;
import nodebox.ui.Zoom;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkNotNull;
import static nodebox.util.ListUtils.listClass;

public class Viewer extends JComponent implements OutputView, Zoom, MouseListener, MouseMotionListener, KeyListener {

    public static final float MIN_ZOOM = 0.01f;
    public static final float MAX_ZOOM = 64.0f;

    private static final ImmutableList<Visualizer> visualizers;
    private static final Visualizer DEFAULT_VISUALIZER = LastResortVisualizer.INSTANCE;

    private static final Cursor defaultCursor, panCursor;

    private final NodeBoxDocument document;
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
                panCursorImage = ImageIO.read(Viewer.class.getResourceAsStream("/view-cursor-pan-32.png"));
            else
                panCursorImage = ImageIO.read(Viewer.class.getResourceAsStream("/view-cursor-pan.png"));
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
        setBackground(Theme.VIEWER_BACKGROUND_COLOR);

        viewerMenu = new JPopupMenu();
        viewerMenu.add(new ResetViewAction());
        PopupHandler popupHandler = new PopupHandler();
        addMouseListener(popupHandler);
    }

    public void zoom(double scaleDelta) {
//        if (! isVisible()) return;
//        double currentScale = getCamera().getViewScale();
//        double newScale = currentScale * scaleDelta;
//        if (newScale < MIN_ZOOM) {
//            scaleDelta = MIN_ZOOM / currentScale;
//        } else if (newScale > MAX_ZOOM) {
//            scaleDelta = MAX_ZOOM / currentScale;
//        }
//        getCamera().scaleViewAboutPoint(scaleDelta, getCamera().getWidth() / 2, getCamera().getHeight() / 2);
    }

    public boolean containsPoint(Point point) {
        if (!isVisible()) return false;
        return getBounds().contains(point);
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
            //viewerLayer.setBounds(-1000000, -1000000, 2000000, 2000000);
            //viewerLayer.setOffset(visualizer.getOffset(outputValues, getSize()));
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
    }

    //// Mouse events ////

    private nodebox.graphics.Point pointForEvent(MouseEvent e) {
        return new nodebox.graphics.Point(e.getX(), e.getY());
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
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Draw background
        g2.setColor(getBackground());
        g2.fill(g.getClipBounds());

        paintObjects(g2);
        paintHandle(g2);
        paintPoints(g2);
        paintPointNumbers(g2);
        paintOrigin(g2);
    }


    public void paintObjects(Graphics2D g) {
        if (currentVisualizer != null)
            currentVisualizer.draw(g, outputValues);
    }

    private void paintPoints(Graphics2D g) {
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


    private void paintPointNumbers(Graphics2D g) {
        if (!showPointNumbers) return;
        g.setFont(Theme.SMALL_MONO_FONT);
        g.setColor(Color.BLUE);
        int index = 0;

        if (IGeometry.class.isAssignableFrom(valuesClass)) {
            for (Object o : outputValues) {
                IGeometry geo = (IGeometry) o;
                for (nodebox.graphics.Point pt : geo.getPoints())
                    paintPointNumber(g, pt, index++);
            }
        } else if (nodebox.graphics.Point.class.isAssignableFrom(valuesClass)) {
            for (Object o : outputValues)
                paintPointNumber(g, (nodebox.graphics.Point) o, index++);
        }
    }

    private void paintPointNumber(Graphics2D g, nodebox.graphics.Point pt, int number) {
        if (pt.isOnCurve()) {
            g.setColor(Color.BLUE);
        } else {
            g.setColor(Color.RED);
        }
        g.drawString(number + "", (int) (pt.x + 3), (int) (pt.y - 2));
    }

    public void paintOrigin(Graphics2D g) {
        // Draw the origin.
        Point2D origin = new Point2D.Double(0, 0);
        int x = (int) Math.round(origin.getX());
        int y = (int) Math.round(origin.getY());
        if (showOrigin) {
            g.setColor(Color.DARK_GRAY);
            g.drawLine(x, 0, x, getHeight());
            g.drawLine(0, y, getWidth(), y);
        }
    }

    public void paintHandle(Graphics2D g) {
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


    private class PopupHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        public void showPopup(MouseEvent e) {
            if (!e.isPopupTrigger()) return;
            viewerMenu.show(Viewer.this, e.getX(), e.getY());
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
