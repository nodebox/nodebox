package nodebox.client;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PPaintContext;
import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Grob;
import nodebox.graphics.IGeometry;
import nodebox.graphics.Path;
import nodebox.handle.Handle;
import nodebox.node.DirtyListener;
import nodebox.node.Node;
import nodebox.node.ProcessingContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

public class Viewer extends PCanvas implements PaneView, DirtyListener, MouseListener, MouseMotionListener, KeyListener {

    public static final float POINT_SIZE = 4f;

    public static final float MIN_ZOOM = 0.1f;
    public static final float MAX_ZOOM = 16.0f;

    private Pane pane;
    private Node node;
    private Node activeNode;
    private Handle handle;
    private boolean showHandle;
    private boolean showPoints;
    private boolean showPointNumbers;
    private PLayer viewerLayer;
    private JPopupMenu viewerMenu;

    public Viewer(Pane pane, Node node) {
        this.pane = pane;
        this.node = node;
        addMouseListener(this);
        addMouseMotionListener(this);
        showHandle = true;
        showPoints = false;
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
        PInputEventFilter panFilter = new PInputEventFilter(InputEvent.BUTTON2_MASK);
        panFilter.setNotMask(InputEvent.CTRL_MASK);
        PPanEventHandler panHandler = new PPanEventHandler();
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

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        if (this.node == node) return;
        Node oldNode = this.node;
        if (oldNode != null) {
            oldNode.removeDirtyListener(this);
        }
        this.node = node;
        if (this.node == null) return;
        node.addDirtyListener(this);
        repaint();
    }

    public void setActiveNode(Node node) {
        activeNode = node;
        if (activeNode != null) {
            handle = activeNode.createHandle();
            if (handle != null) {
                handle.setViewer(this);
            }
        } else {
            handle = null;
        }
        repaint();
    }

    public boolean hasVisibleHandle() {
        if (handle == null) return false;
        if (showHandle == false) return false;
        return handle.isVisible();
    }


    //// Network data events ////

    public void nodeDirty(Node node) {
        // The node is dirty, but we wait for the document to update the network.
        // This will send the nodeUpdated event.
    }

    public void nodeUpdated(Node node, ProcessingContext context) {
        if (node != getNode()) return;
        // Note that we don't use check handle visibility here, since the update might change handle visibility.
        if (handle != null && showHandle) {
            handle.update();
        }
        // Set bounds from output value.
        if (getNode() != null) {
            Object outputValue = getNode().getOutputValue();
            if (outputValue instanceof Grob) {
                Grob grob = (Grob) outputValue;
                viewerLayer.setBounds(grob.getBounds().getRectangle2D());
                viewerLayer.setOffset(getWidth() / 2, getHeight() / 2);
            } else if (outputValue != null) {
                resetView();
                viewerLayer.setBounds(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
                viewerLayer.setOffset(5, 5);
            }
        }
        repaint();
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
        double cx = -getWidth() / 2.0 + transformedPoint.getX();
        double cy = -getHeight() / 2.0 + transformedPoint.getY();
        return new nodebox.graphics.Point((float) cx, (float) cy);
    }

    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger()) return;
        requestFocus();
        if (hasVisibleHandle())
            handle.mouseClicked(pointForEvent(e));
    }

    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) return;
        requestFocus();
        if (hasVisibleHandle())
            handle.mousePressed(pointForEvent(e));
    }

    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) return;
        requestFocus();
        if (hasVisibleHandle())
            handle.mouseReleased(pointForEvent(e));
    }

    public void mouseEntered(MouseEvent e) {
        if (e.isPopupTrigger()) return;
        requestFocus();
        if (hasVisibleHandle())
            handle.mouseEntered(pointForEvent(e));
    }

    public void mouseExited(MouseEvent e) {
        if (e.isPopupTrigger()) return;
        requestFocus();
        if (hasVisibleHandle())
            handle.mouseExited(pointForEvent(e));
    }

    public void mouseDragged(MouseEvent e) {
        if (e.isPopupTrigger()) return;
        requestFocus();
        if (hasVisibleHandle())
            handle.mouseDragged(pointForEvent(e));
    }

    public void mouseMoved(MouseEvent e) {
        if (e.isPopupTrigger()) return;
        requestFocus();
        if (hasVisibleHandle())
            handle.mouseMoved(pointForEvent(e));
    }

    public void keyTyped(KeyEvent e) {
        if (hasVisibleHandle())
            handle.keyTyped(e.getKeyCode(), e.getModifiersEx());
    }

    public void keyPressed(KeyEvent e) {
        if (hasVisibleHandle())
            handle.keyPressed(e.getKeyCode(), e.getModifiersEx());
    }

    public void keyReleased(KeyEvent e) {
        if (hasVisibleHandle())
            handle.keyReleased(e.getKeyCode(), e.getModifiersEx());
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    public class ViewerLayer extends PLayer {

        @Override
        protected void paint(PPaintContext paintContext) {
            Graphics2D g2 = paintContext.getGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fill the background with a neutral grey.
            //g2.setColor(new Color(232, 232, 232));
            //Rectangle clip = g2.getClipBounds();
            //g2.fillRect(clip.x, clip.y, clip.width, clip.height);

            //if (canvasImage != null)
            //g2.drawImage(canvasImage,0, 0, null);

            if (getNode() == null) return;
            Object outputValue = getNode().getOutputValue();
            if (outputValue instanceof Grob) {
                ((Grob) outputValue).draw(g2);
            } else if (outputValue != null) {
                String s = outputValue.toString();
                g2.setColor(Theme.TEXT_NORMAL_COLOR);
                g2.setFont(Theme.EDITOR_FONT);
                g2.drawString(s, 5, 20);
            }

            // Draw the handle.
            if (hasVisibleHandle()) {
                // Create a canvas with a transparent background
                nodebox.graphics.Canvas canvas = new nodebox.graphics.Canvas();
                canvas.setBackground(new nodebox.graphics.Color(0, 0, 0, 0));
                GraphicsContext ctx = new GraphicsContext(canvas);
                handle.draw(ctx);
                ctx.getCanvas().draw(g2);
            }

            // Draw the points.
            if (showPoints && outputValue instanceof IGeometry) {
                // Create a canvas with a transparent background
                Path onCurves = new Path();
                Path offCurves = new Path();
                onCurves.setFill(new nodebox.graphics.Color(0f, 0f, 1f));
                offCurves.setFill(new nodebox.graphics.Color(1f, 0f, 0f));
                IGeometry p = (IGeometry) outputValue;
                for (nodebox.graphics.Point pt : p.getPoints()) {
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
            if (showPointNumbers && outputValue instanceof IGeometry) {
                g2.setFont(Theme.SMALL_MONO_FONT);
                g2.setColor(Color.BLUE);
                // Create a canvas with a transparent background
                IGeometry p = (IGeometry) outputValue;
                int index = 0;
                for (nodebox.graphics.Point pt : p.getPoints()) {
                    if (pt.isOnCurve()) {
                        g2.setColor(Color.BLUE);
                    } else {
                        g2.setColor(Color.RED);
                    }
                    g2.drawString(index + "", pt.x + 3, pt.y - 2);
                    index++;
                }
            }

            // Draw the center.
            //g.setColor(new Color(240, 240, 240));
            //g.drawLine(-getWidth() / 2, 0, getWidth() / 2, 0);
            //g.drawLine(0, -getHeight() / 2, 0, getHeight() / 2);

            // Draw the bounding box.
            //if (outputValue instanceof IGeometry) {
            //    IGeometry p = (IGeometry) outputValue;
            //    g2.setColor(Color.BLUE);
            //    g2.draw(p.getBounds().getRectangle2D());
            //}
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
