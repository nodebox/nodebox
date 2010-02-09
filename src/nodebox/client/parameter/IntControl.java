package nodebox.client.parameter;

import nodebox.client.DraggableNumber;
import nodebox.node.Parameter;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

public class IntControl extends AbstractParameterControl implements ChangeListener, ActionListener {

    private DraggableNumber draggable;

    public IntControl(Parameter parameter) {
        super(parameter);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        draggable = new DraggableNumber();
        draggable.addChangeListener(this);
        NumberFormat intFormat = NumberFormat.getNumberInstance();
        intFormat.setMinimumFractionDigits(0);
        intFormat.setMaximumFractionDigits(0);
        draggable.setNumberFormat(intFormat);
        // Set bounding
        if (parameter.getBoundingMethod() == Parameter.BoundingMethod.HARD) {
            Float minimumValue = parameter.getMinimumValue();
            if (minimumValue != null)
                draggable.setMinimumValue(minimumValue);
            Float maximumValue = parameter.getMaximumValue();
            if (maximumValue != null)
                draggable.setMaximumValue(maximumValue);
        }
        add(draggable);
        setPreferredSize(draggable.getPreferredSize());
        setValueForControl(parameter.getValue());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        draggable.setEnabled(enabled);
    }

    public void setValueForControl(Object v) {
        int value = (Integer) v;
        draggable.setValue(value);
    }

    public void stateChanged(ChangeEvent e) {
        setValueFromControl();
    }

    public void actionPerformed(ActionEvent e) {
        setValueFromControl();
    }

    private void setValueFromControl() {
        double doubleValue = draggable.getValue();
        if (parameter.getBoundingMethod() == Parameter.BoundingMethod.HARD) {
            if (parameter.getMinimumValue() != null) {
                doubleValue = Math.max(parameter.getMinimumValue(), doubleValue);
            }
            if (parameter.getMaximumValue() != null) {
                doubleValue = Math.min(parameter.getMaximumValue(), doubleValue);
            }
        }
        int intValue = (int) doubleValue;
        if (intValue != parameter.asInt()) {
            parameter.setValue(intValue);
        }
    }

}
