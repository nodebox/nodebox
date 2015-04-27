package nodebox.client;

import com.google.common.collect.ImmutableList;
import nodebox.client.visualizer.*;
import nodebox.graphics.CanvasContext;
import nodebox.graphics.IGeometry;
import nodebox.handle.Handle;
import nodebox.ui.Theme;
import nodebox.ui.Zoom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkNotNull;
import static nodebox.util.ListUtils.listClass;

public class Viewer extends ZoomableView implements OutputView, Zoom, MouseListener, MouseMotionListener, KeyListener {

    public static final double MIN_ZOOM = 0.01;
    public static final double MAX_ZOOM = 64.0;

    private final JPopupMenu viewerMenu;

    private nodebox.graphics.Point lastMousePosition = nodebox.graphics.Point.ZERO;

    private Handle handle;
    private boolean showHandle = true;
    private boolean showPoints = false;
    private boolean showPointNumbers = false;
    private boolean showOrigin = false;
    private boolean showBounds = false;
    private boolean viewPositioned = false;

    private java.util.List<?> outputValues;
    private Rectangle2D canvasBounds = new Rectangle2D.Double(-500, -500, 1000, 1000);
    private Class valuesClass;
    private Visualizer currentVisualizer = VisualizerFactory.getDefaultVisualizer();

    public Viewer() {
        super(MIN_ZOOM, MAX_ZOOM);
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
        super.zoom(scaleDelta, getWidth() / 2.0, getHeight() / 2.0);
    }

    public boolean containsPoint(Point point) {
        return isVisible() && getBounds().contains(point);
    }

    public void setShowHandle(boolean showHandle) {
        this.showHandle = showHandle;
        repaint();
    }

    public void setShowPoints(boolean showPoints) {
        this.showPoints = showPoints;
        repaint();
    }

    public void setShowPointNumbers(boolean showPointNumbers) {
        this.showPointNumbers = showPointNumbers;
        repaint();
    }

    public void setShowOrigin(boolean showOrigin) {
        this.showOrigin = showOrigin;
        repaint();
    }

    public void setShowBounds(boolean showBounds) {
        this.showBounds = showBounds;
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

        // Don't show handles for LastResortVisualizer and ColorVisualizer.
        if (currentVisualizer instanceof LastResortVisualizer) return false;
        if (currentVisualizer instanceof ColorVisualizer) return false;

        return handle.isVisible();
    }

    //// Network data events ////

    public void setOutputValues(java.util.List<?> outputValues) {
        this.outputValues = outputValues;
        valuesClass = listClass(outputValues);
        Visualizer visualizer = VisualizerFactory.getVisualizer(outputValues, valuesClass);
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
            currentVisualizer = visualizer;
            resetViewTransform();
        }
        checkNotNull(currentVisualizer);
        repaint();
    }

    public void setCanvasBounds(Rectangle2D bounds) {
        this.canvasBounds = bounds;
        repaint();
    }

    @Override
    public void resetViewTransform() {
        Point2D position = currentVisualizer.getOffset(outputValues, getSize());
        setViewTransform(position.getX(), position.getY(), 1);
    }

    //// Mouse events ////

    private nodebox.graphics.Point pointForEvent(MouseEvent e) {
        Point2D pt = inverseViewTransformPoint(e.getPoint());
        return new nodebox.graphics.Point(pt);
    }

    public nodebox.graphics.Point getLastMousePosition() {
        return lastMousePosition;
    }

    public void mouseClicked(MouseEvent e) {
        // We register the mouse click as an edit since it can trigger a change to the node.
        if (e.isPopupTrigger()) return;
        if (handle != null)
            handle.mouseClicked(pointForEvent(e));
    }

    public void mousePressed(MouseEvent e) {
        // We register the mouse press as an edit since it can trigger a change to the node.
        if (e.isPopupTrigger()) return;
        if (handle != null)
            handle.mousePressed(pointForEvent(e));
    }

    public void mouseReleased(MouseEvent e) {
        // We register the mouse release as an edit since it can trigger a change to the node.
        if (e.isPopupTrigger()) return;
        if (handle != null)
            handle.mouseReleased(pointForEvent(e));
    }

    public void mouseEntered(MouseEvent e) {
        // Entering the viewer with your mouse should not change the node, so we do not register an edit.
        if (e.isPopupTrigger()) return;
        if (handle != null)
            handle.mouseEntered(pointForEvent(e));
    }

    public void mouseExited(MouseEvent e) {
        // Exiting the viewer with your mouse should not change the node, so we do not register an edit.
        if (e.isPopupTrigger()) return;
        if (handle != null)
            handle.mouseExited(pointForEvent(e));
    }

    public void mouseDragged(MouseEvent e) {
        // We register the mouse drag as an edit since it can trigger a change to the node.
        if (e.isPopupTrigger()) return;
        if (isPanning()) return;
        if (handle != null)
            handle.mouseDragged(pointForEvent(e));
        lastMousePosition = pointForEvent(e);
    }

    public void mouseMoved(MouseEvent e) {
        // Moving the mouse in the viewer area should not change the node, so we do not register an edit.
        if (e.isPopupTrigger()) return;
        if (handle != null)
            handle.mouseMoved(pointForEvent(e));
        lastMousePosition = pointForEvent(e);
    }

    public void keyTyped(KeyEvent e) {
        if (handle != null)
            handle.keyTyped(e.getKeyCode(), e.getModifiersEx());
    }

    public void keyPressed(KeyEvent e) {
        if (handle != null)
            handle.keyPressed(e.getKeyCode(), e.getModifiersEx());
    }

    public void keyReleased(KeyEvent e) {
        Component c = SwingUtilities.getWindowAncestor(Viewer.this);
        if (c instanceof FullScreenFrame) {
            FullScreenFrame frame = (FullScreenFrame) c;
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                frame.close();
            } else if (e.getKeyCode() == KeyEvent.VK_P) {
                int metaMask = KeyEvent.META_MASK;
                int metaShiftMask = KeyEvent.META_MASK | KeyEvent.SHIFT_MASK;
                if (e.getModifiers() == metaMask)
                    frame.toggleAnimation();
                else if (e.getModifiers() == metaShiftMask)
                    frame.rewindAnimation();
            }
        }

        if (handle != null)
            handle.keyReleased(e.getKeyCode(), e.getModifiersEx());
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public void paintComponent(Graphics g) {
        if (!viewPositioned) {
            setViewPosition(getWidth() / 2.0, getHeight() / 2.0);
            viewPositioned = true;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Draw background
        g2.setColor(getBackground());
        g2.fill(g.getClipBounds());

        // Set the view transform
        AffineTransform originalTransform = g2.getTransform();
        g2.transform(getViewTransform());

        paintBounds(g2);
        paintObjects(g2);
        paintHandle(g2);
        paintPoints(g2);
        paintPointNumbers(g2);

        // Restore original transform
        g2.setClip(null);
        g2.setTransform(originalTransform);
        g2.setStroke(new BasicStroke(1));

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
        if (showOrigin) {
            int x = (int) Math.round(getViewX());
            int y = (int) Math.round(getViewY());
            g.setColor(Color.DARK_GRAY);
            g.drawLine(x, 0, x, getHeight());
            g.drawLine(0, y, getWidth(), y);
        }
    }

    public void paintBounds(Graphics2D g) {
        if (showBounds) {
            g.setColor(Color.DARK_GRAY);
            int x = (int) Math.round(canvasBounds.getX());
            int y = (int) Math.round(canvasBounds.getY());
            int width = (int) Math.round(canvasBounds.getWidth());
            int height = (int) Math.round(canvasBounds.getHeight());
            g.drawRect(x, y, width, height);
            g.drawLine(x + width + 1, y + 1, x + width + 1, y + height + 1);
            g.drawLine(x + 1, y + height + 1, x + width + 1, y + height + 1);
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
            resetViewTransform();
        }
    }

}
