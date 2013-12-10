package nodebox.client;

import nodebox.ui.Platform;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.IOException;

public class ZoomableView extends JComponent {

    private static Cursor defaultCursor, panCursor;
    private final double minZoom, maxZoom;
    // View state
    private double viewX, viewY, viewScale = 1;
    private transient AffineTransform viewTransform = null;
    private transient AffineTransform inverseViewTransform = null;
    // Interaction state
    private boolean isSpacePressed = false;
    private boolean isPanning = false;
    private Point2D dragStartPoint;
    private boolean isZooming = false;
    private Point2D zoomStartPoint;
    private Point2D zoomEndPoint;

    static {
        Image panCursorImage;

        try {
            if (Platform.onWindows())
                panCursorImage = ImageIO.read(NetworkView.class.getResourceAsStream("/view-cursor-pan-32.png"));
            else
                panCursorImage = ImageIO.read(NetworkView.class.getResourceAsStream("/view-cursor-pan.png"));
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            panCursor = toolkit.createCustomCursor(panCursorImage, new Point(0, 0), "PanCursor");
            defaultCursor = Cursor.getDefaultCursor();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ZoomableView(double minZoom, double maxZoom) {
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        setFocusable(true);
        addKeyListener(new KeyHandler());
        addMouseListener(new MouseHandler());
        addMouseMotionListener(new MouseMotionHandler());
        addMouseWheelListener(new MouseWheelHandler());
        final FocusHandler fh = new FocusHandler();
        addFocusListener(fh);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.getWindowAncestor(ZoomableView.this).addWindowFocusListener(fh);
            }
        });
    }

    public double getViewX() {
        return viewX;
    }

    public void setViewPosition(double x, double y) {
        viewX = x;
        viewY = y;
        repaint();
    }

    public double getViewY() {
        return viewY;
    }

    public double getViewScale() {
        return viewScale;
    }

    public boolean isSpacePressed() {
        return isSpacePressed;
    }

    public boolean isDragTrigger(MouseEvent e) {
        return isSpacePressed();
    }

    public boolean isPanning() {
        return isPanning;
    }

    public boolean isZooming() {
        return isZooming;
    }

    private Point2D minPoint(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() - b.getX(), a.getY() - b.getY());
    }

    public void setViewTransform(double viewX, double viewY, double viewScale) {
        this.viewX = viewX;
        this.viewY = viewY;
        this.viewScale = viewScale;
        onViewTransformChanged(viewX, viewY, viewScale);
        this.viewTransform = null;
        this.inverseViewTransform = null;
        repaint();
    }

    protected void onViewTransformChanged(double viewX, double viewY, double viewScale) {
    }

    public AffineTransform getViewTransform() {
        if (viewTransform == null) {
            viewTransform = new AffineTransform();
            viewTransform.translate(viewX, viewY);
            viewTransform.scale(viewScale, viewScale);
        }
        return viewTransform;
    }

    //// View Transform ////

    public AffineTransform getInverseViewTransform() {
        if (inverseViewTransform == null) {
            try {
                inverseViewTransform = getViewTransform().createInverse();
            } catch (NoninvertibleTransformException e) {
                inverseViewTransform = new AffineTransform();
            }
        }
        return inverseViewTransform;
    }

    public void resetViewTransform() {
        setViewTransform(0, 0, 1);
    }

    public Point2D inverseViewTransformPoint(Point p) {
        Point2D pt = new Point2D.Double(p.getX(), p.getY());
        return getInverseViewTransform().transform(pt, null);
    }

    public void zoom(double scaleDelta, double x, double y) {
        if (!isVisible()) return;
        double currentScale = getViewScale();
        double newScale = currentScale * scaleDelta;
        if (newScale < minZoom) {
            scaleDelta = minZoom / viewScale;
        } else if (newScale > maxZoom) {
            scaleDelta = maxZoom / viewScale;
        }
        double vx = viewX - (x - viewX) * (scaleDelta - 1);
        double vy = viewY - (y - viewY) * (scaleDelta - 1);
        setViewTransform(vx, vy, viewScale * scaleDelta);
    }

    private class KeyHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_SPACE) {
                isSpacePressed = true;
                setCursor(panCursor);
            }

        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                isSpacePressed = false;
                setCursor(defaultCursor);
            }
        }
    }

    private class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) return;

            // Pan using the middle mouse button.
            if (e.getButton() == MouseEvent.BUTTON2) { //middle mouse pan, does same as below
                dragStartPoint = e.getPoint();
                isPanning = true;
                return;
            }

            // Zoom by pressing middle mouse button, then left mouse button.
            if (isPanning && e.getButton() == MouseEvent.BUTTON1) {
                // Center point of the zoom, doesn't change.
                zoomStartPoint = e.getPoint();
                // The distance the mouse will be dragged when zooming.
                zoomEndPoint = e.getPoint();
                isZooming = true;
                return;
            }

            // If the space bar and mouse is pressed, we're getting ready to pan the view.
            if (isSpacePressed) {
                // When panning the view use the original mouse point, not the one affected by the view transform.
                dragStartPoint = e.getPoint();
                isPanning = true;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (isZooming && e.getButton() == MouseEvent.BUTTON1) {
                isZooming = false;
                // Reset pan start point when zoom is finished to avoid a jump.
                dragStartPoint = e.getPoint();
                return;
            }
            isPanning = false;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            requestFocusInWindow();
        }
    }

    private class MouseMotionHandler extends MouseMotionAdapter {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (isZooming()) {
                // How far have we moved?
                Point2D offset = minPoint(e.getPoint(), zoomEndPoint);
                // Drag mouse left to zoom out, right to zoom in.
                zoom(1 + offset.getX() / 200.0, zoomStartPoint.getX(), zoomStartPoint.getY());
                // Reset the end point because zoom() takes incremental (delta) values
                zoomEndPoint = e.getPoint();
                return;
            }

            if (isPanning()) {
                // When panning the view use the original mouse point, not the one affected by the view transform.
                Point2D offset = minPoint(e.getPoint(), dragStartPoint);
                setViewTransform(viewX + offset.getX(), viewY + offset.getY(), viewScale);
                dragStartPoint = e.getPoint();
            }
        }
    }

    private class MouseWheelHandler implements MouseWheelListener {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            double scaleDelta = 1.0 - (e.getWheelRotation() / 10.0);
            zoom(scaleDelta, e.getX(), e.getY());
        }
    }

    private class FocusHandler implements WindowFocusListener, FocusListener {
        @Override
        public void windowLostFocus(WindowEvent e) {
            isSpacePressed = false;
            isPanning = false;
            setCursor(defaultCursor);
        }

        @Override
        public void windowGainedFocus(WindowEvent e) {
        }

        @Override
        public void focusGained(FocusEvent e) {
        }

        @Override
        public void focusLost(FocusEvent e) {
            isSpacePressed = false;
            setCursor(defaultCursor);
        }
    }
}
