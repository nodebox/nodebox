package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class NSplitter extends JPanel {

    public static final String POSITION = "position";
    private static final float MIDDLE_POSITION = .5f;

    enum Orientation {
        HORIZONTAL, VERTICAL
    }

    private Orientation orientation;
    private JComponent firstComponent;
    private JComponent secondComponent;
    private Divider divider;
    private float position;
    private float minimumPosition;
    private float maximumPosition;
    private int dividerSize = 5;

    public NSplitter() {
        this(Orientation.HORIZONTAL);
    }

    public NSplitter(Orientation orientation) {
        this.orientation = orientation;
        this.divider = createDivider();
        this.position = 0.5f;
        this.minimumPosition = 0.0f;
        this.maximumPosition = 1.0f;
        super.add(divider);
    }

    public NSplitter(Orientation orientation, JComponent firstComponent, JComponent secondComponent) {
        this(orientation);
        setFirstComponent(firstComponent);
        setSecondComponent(secondComponent);
    }

    protected Divider createDivider() {
        return new Divider();
    }

    public float getPosition() {
        return position;
    }

    public void setPosition(float position) {
        if (this.position == position) return;
        if (position < .0f || position > 1.0f)
            throw new IllegalArgumentException("Wrong position: " + position);
        if (position < minimumPosition) position = minimumPosition;
        if (position > maximumPosition) position = maximumPosition;
        float oldPosition = position;
        this.position = position;
        firePropertyChange(POSITION, new Float(oldPosition), new Float(position));
        doLayout();
        repaint();
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
        doLayout();
        repaint();
    }

    public int getDividerSize() {
        return dividerSize;
    }

    public void setDividerSize(int dividerSize) {
        if (dividerSize <= 0)
            throw new IllegalArgumentException("Wrong divider size: " + dividerSize);
        if (this.dividerSize == dividerSize) return;
        this.dividerSize = dividerSize;
        doLayout();
        repaint();
    }

    public int getAbsolutePosition() {
        if (orientation == Orientation.VERTICAL) {
            return divider.getY();
        } else {
            return divider.getX();
        }
    }

    @Override
    public Component add(Component comp) {
        throw new UnsupportedOperationException("Use setFirstComponent or setSecondComponent to change components.");
    }

    @Override
    public void remove(Component comp) {
        throw new UnsupportedOperationException("Use setFirstComponent or setSecondComponent to change components.");
    }

    @Override
    public void doLayout() {
        final int width = getWidth();
        final int height = getHeight();
        if (firstComponent != null && secondComponent != null) {
            Rectangle firstRect = new Rectangle();
            Rectangle dividerRect = new Rectangle();
            Rectangle secondRect = new Rectangle();
            final int componentSize = getOrientation() == Orientation.VERTICAL ? height : width;
            int firstComponentSize, secondComponentSize;
            int dividerWidth = getDividerSize();
            if (componentSize <= dividerWidth) {
                firstComponentSize = 0;
                secondComponentSize = 0;
                dividerWidth = componentSize;
            } else {
                firstComponentSize = (int) (position * (float) (componentSize - dividerWidth));
                secondComponentSize = getOrientation() == Orientation.VERTICAL ? height - firstComponentSize - dividerWidth : width - firstComponentSize - dividerWidth;
            }
            if (orientation == Orientation.VERTICAL) {
                firstRect.setBounds(0, 0, width, firstComponentSize);
                dividerRect.setBounds(0, firstComponentSize, width, dividerWidth);
                secondRect.setBounds(0, firstComponentSize + dividerWidth, width, secondComponentSize);
            } else {
                firstRect.setBounds(0, 0, firstComponentSize, height);
                dividerRect.setBounds(firstComponentSize, 0, dividerWidth, height);
                secondRect.setBounds(firstComponentSize + dividerWidth, 0, secondComponentSize, height);
            }
            divider.setVisible(true);
            firstComponent.setBounds(firstRect);
            divider.setBounds(dividerRect);
            secondComponent.setBounds(secondRect);
            firstComponent.validate();
            secondComponent.validate();
        } else if (firstComponent != null && secondComponent == null) {
            divider.setVisible(false);
            firstComponent.setBounds(0, 0, width, height);
            firstComponent.validate();
        } else if (firstComponent == null && secondComponent != null) {
            divider.setVisible(false);
            secondComponent.setBounds(0, 0, width, height);
            secondComponent.validate();
        } else {
            divider.setVisible(false);
        }
        divider.doLayout();
    }

    public JComponent getFirstComponent() {
        return firstComponent;
    }

    public void setFirstComponent(JComponent firstComponent) {
        if (this.firstComponent == firstComponent) return;
        if (this.firstComponent != null) {
            super.remove(this.firstComponent);
        }
        this.firstComponent = firstComponent;
        if (this.firstComponent != null) {
            super.add(firstComponent);
            firstComponent.invalidate();
        }
    }

    public JComponent getSecondComponent() {
        return secondComponent;
    }

    public void setSecondComponent(JComponent secondComponent) {
        if (this.secondComponent == secondComponent) return;
        if (this.secondComponent != null) {
            super.remove(this.secondComponent);
        }
        this.secondComponent = secondComponent;
        if (this.secondComponent != null) {
            super.add(secondComponent);
            secondComponent.invalidate();
        }
    }

    public Divider getDivider() {
        return divider;
    }

    protected class Divider extends JComponent {

        private boolean dragging;
        private Point dragPoint;

        public Divider() {
            setFocusable(false);
            enableEvents(MouseEvent.MOUSE_EVENT_MASK | MouseEvent.MOUSE_MOTION_EVENT_MASK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Rectangle r = getBounds();
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(Color.GRAY);
            if (getOrientation() == Orientation.VERTICAL) {
                g.drawLine(0, 0, r.width, 0);
                g.drawLine(0, dividerSize - 1, r.width, dividerSize - 1);
            } else {
                g.drawLine(0, 0, 0, r.height);
                g.drawLine(dividerSize - 1, 0, dividerSize - 1, r.height);
            }
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            super.processMouseEvent(e);
            if (!NSplitter. this.isEnabled()) return;
            switch (e.getID()) {
                case MouseEvent.MOUSE_ENTERED:
                    setCursor(getOrientation() == Orientation.VERTICAL ? Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR) : Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    break;
                case MouseEvent.MOUSE_EXITED:
                    if (dragging) break;
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    break;
                case MouseEvent.MOUSE_PRESSED:
                    setCursor(getOrientation() == Orientation.VERTICAL ? Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR) : Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    dragging = false;
                    dragPoint = null;
                    break;
                case MouseEvent.MOUSE_CLICKED:
                    if (e.getClickCount() != 2) {
                        setPosition(MIDDLE_POSITION);
                    }
                    break;
            }
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            super.processMouseMotionEvent(e);
            if (!NSplitter. this.isEnabled()) return;
            if (MouseEvent.MOUSE_DRAGGED != e.getID()) return;
            dragging = true;
            setCursor(getOrientation() == Orientation.VERTICAL ? Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR) : Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            dragPoint = SwingUtilities.convertPoint(this, e.getPoint(), NSplitter.this);
            float position;
            if (getOrientation() == Orientation.VERTICAL) {
                position = Math.min(1.0f, Math.max(minimumPosition, (float) dragPoint.y / (float) NSplitter.this.getHeight()));
                setPosition(position);
            } else {
                position = Math.min(1.0f, Math.max(minimumPosition, (float) dragPoint.x / (float) NSplitter.this.getWidth()));
                setPosition(position);
            }
        }
    }
}
