package nodebox.client.port;

import nodebox.graphics.Point;
import nodebox.node.Port;
import nodebox.ui.DraggableNumber;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.google.common.base.Preconditions.checkArgument;

public class PointControl extends AbstractPortControl implements ChangeListener, ActionListener {

    private final DraggableNumber xNumber;
    private final DraggableNumber yNumber;

    public PointControl(String nodePath, Port port) {
        super(nodePath, port);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        xNumber = new DraggableNumber();
        xNumber.addChangeListener(this);
        yNumber = new DraggableNumber();
        yNumber.addChangeListener(this);
        add(xNumber);
        add(Box.createHorizontalStrut(5));
        add(yNumber);
        setValueForControl(port.getValue());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        xNumber.setEnabled(enabled);
        yNumber.setEnabled(enabled);
    }

    public void setValueForControl(Object v) {
        checkArgument(v instanceof Point);
        nodebox.graphics.Point pt = (Point) v;
        xNumber.setValue(pt.getX());
        yNumber.setValue(pt.getY());
    }

    public void stateChanged(ChangeEvent e) {
        setValueFromControl();
    }

    public void actionPerformed(ActionEvent e) {
        setValueFromControl();
    }

    private void setValueFromControl() {
        double x = xNumber.getValue();
        double y = yNumber.getValue();
        setPortValue(new Point(x, y));
    }

}
