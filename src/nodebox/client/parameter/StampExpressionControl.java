package nodebox.client.parameter;

import nodebox.client.Theme;
import nodebox.node.Parameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StampExpressionControl extends AbstractParameterControl implements ActionListener {

    private JTextField keyField;
    private JTextField expressionField;

    public StampExpressionControl(Parameter parameter) {
        super(parameter);
        setLayout(new BorderLayout(5, 0));
        keyField = new JTextField();
        keyField.putClientProperty("JComponent.sizeVariant", "small");
        keyField.setFont(Theme.SMALL_BOLD_FONT);
        keyField.addActionListener(this);
        keyField.setPreferredSize(new Dimension(75, 0));
        add(keyField, BorderLayout.WEST);
        expressionField = new JTextField();
        expressionField.putClientProperty("JComponent.sizeVariant", "small");
        expressionField.setFont(Theme.SMALL_BOLD_FONT);
        expressionField.addActionListener(this);
        add(expressionField, BorderLayout.CENTER);
        setValueForControl(parameter.getValue());
    }

    public void setValueForControl(Object v) {
        if (v == null) return;
        String s = v.toString();
        int equalsPos = s.indexOf('=');
        if (equalsPos < 0) {
            keyField.setText("");
            expressionField.setText("");
        } else {
            String key = s.substring(0, equalsPos);
            String expression = s.substring(equalsPos + 1);
            keyField.setText(key);
            expressionField.setText(expression);
        }
    }

    public void actionPerformed(ActionEvent e) {
        String newValue = keyField.getText() + "=" + expressionField.getText();
        if (!newValue.equals(parameter.asString())) {
            parameter.set(newValue);
        }
    }

}
