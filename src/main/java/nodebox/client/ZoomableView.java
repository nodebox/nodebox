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
        addFocusListener(new FocusHandler());
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

    private Point2D minPoint(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() - b.getX(), a.getY() - b.getY());
    }

    public void setViewTransform(double viewX, double viewY, double viewScale) {
        this.viewX = viewX;
        this.viewY = viewY;
        this.viewScale = viewScale;
        this.viewTransform = null;
        this.inverseViewTransform = null;
        repaint();
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
            // If the space bar and mouse is pressed, we're getting ready to pan the view.
            if (isSpacePressed) {
                // When panning the view use the original mouse point, not the one affected by the view transform.
                dragStartPoint = e.getPoint();
                isPanning = true;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            isPanning = false;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            grabFocus();
        }
    }

    private class MouseMotionHandler extends MouseMotionAdapter {
        @Override
        public void mouseDragged(MouseEvent e) {
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

    private class FocusHandler extends FocusAdapter {
        @Override
        public void focusLost(FocusEvent e) {
            isSpacePressed = false;
            isPanning = false;
            setCursor(defaultCursor);
        }
    }
}
