package net.nodebox.client;

import net.nodebox.Icons;
import net.nodebox.node.ConnectionError;
import net.nodebox.node.Parameter;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

public class ParameterRow extends JComponent implements ComponentListener {

    private Parameter parameter;
    private JLabel label;
    private JComponent control;
    private JTextField expressionField;
    private JButton popupButton;
    private JCheckBoxMenuItem expressionMenuItem;
    private Color borderColor = new Color(170, 170, 170);

    private static final int TOP_PADDING = 2;
    private static final int BOTTOM_PADDING = 2;

    public ParameterRow(Parameter parameter, JComponent control) {
        addComponentListener(this);
        this.parameter = parameter;

        setLayout(null);

        label = new JLabel(parameter.getLabel());
        label.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
        label.setHorizontalAlignment(JLabel.RIGHT);
        label.setHorizontalTextPosition(JLabel.RIGHT);
        label.putClientProperty("JComponent.sizeVariant", "small");
        label.setFont(PlatformUtils.getSmallBoldFont());
        label.setToolTipText(parameter.getName());

        this.control = control;

        popupButton = new JButton();
        popupButton.putClientProperty("JButton.buttonType", "roundRect");
        popupButton.putClientProperty("JComponent.sizeVariant", "mini");
        popupButton.setFocusable(false);
        popupButton.setIcon(new Icons.ArrowIcon(Icons.ArrowIcon.SOUTH, new Color(180, 180, 180)));
        JPopupMenu menu = new JPopupMenu();
        expressionMenuItem = new JCheckBoxMenuItem(new ToggleExpressionAction());
        menu.add(expressionMenuItem);
        menu.add(new RevertToDefaultAction());
        menu.add(new DisconnectAction());
        popupButton.setComponentPopupMenu(menu);
        popupButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                JButton b = (JButton) e.getSource();
                Point p = e.getPoint();
                b.getComponentPopupMenu().show(b, p.x, p.y);
            }
        });

        expressionField = new JTextField();
        expressionField.setVisible(false);
        expressionField.setAction(new ExpressionFieldChangedAction());
        expressionField.putClientProperty("JComponent.sizeVariant", "small");
        expressionField.setFont(PlatformUtils.getSmallBoldFont());

        add(this.label);
        add(this.control);
        add(this.popupButton);
        add(this.expressionField);
        componentResized(null);
        setExpressionStatus();

        setBorder(new Border() {
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                g.setColor(borderColor);
                g.fillRect(x, y + height - 1, width, 1);
            }

            public Insets getBorderInsets(Component c) {
                return new Insets(0, 0, 1, 0);
            }

            public boolean isBorderOpaque() {
                return true;
            }
        });
    }

//
//    @Override
//    public Insets getInsets() {
//        return new Insets(TOP_PADDING, 0, BOTTOM_PADDING, 0);
//    }

    //// Component listeners ////

    public void componentResized(ComponentEvent e) {
        Dimension controlSize = control.getPreferredSize();
        Rectangle bounds = getBounds();
        int h = bounds.height - TOP_PADDING - BOTTOM_PADDING;
        label.setBounds(0, TOP_PADDING, 100, h);
        control.setBounds(110, TOP_PADDING, controlSize.width, h);
        control.doLayout();
        expressionField.setBounds(110, TOP_PADDING, 200, h);
        popupButton.setBounds(bounds.width - 30, TOP_PADDING, 30, h);
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(borderColor);
        g.fillRect(105, 0, 1, getPreferredSize().height);
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, control.getPreferredSize().height + TOP_PADDING + BOTTOM_PADDING);
    }

    //// Parameter context menu ////

    private void setExpressionStatus() {
        if (parameter.hasExpression()) {
            control.setVisible(false);
            expressionField.setVisible(true);
            expressionField.setText(parameter.getExpression());

        } else {
            control.setVisible(true);
            expressionField.setVisible(false);
        }
        expressionMenuItem.setState(parameter.hasExpression());
    }

    //// Action classes ////

    private class ToggleExpressionAction extends AbstractAction {
        private ToggleExpressionAction() {
            putValue(Action.NAME, "Toggle Expression");
        }

        public void actionPerformed(ActionEvent e) {
            if (parameter.hasExpression()) {
                parameter.clearExpression();
            } else {
                parameter.setExpression(parameter.asExpression());
            }
            setExpressionStatus();
        }
    }

    private class RevertToDefaultAction extends AbstractAction {
        private RevertToDefaultAction() {
            putValue(Action.NAME, "Revert to Default");
        }

        public void actionPerformed(ActionEvent e) {
            parameter.revertToDefault();
        }
    }

    private class DisconnectAction extends AbstractAction {
        private DisconnectAction() {
            putValue(Action.NAME, "Disconnect");
        }

        public void actionPerformed(ActionEvent e) {
            parameter.disconnect();
        }
    }

    private class ExpressionFieldChangedAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            try {
                parameter.setExpression(expressionField.getText());
            } catch (ConnectionError ce) {
                JOptionPane.showMessageDialog(ParameterRow.this, ce.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}
