package net.nodebox.client;

import net.nodebox.Icons;
import net.nodebox.node.ConnectionError;
import net.nodebox.node.Parameter;
import sun.swing.SwingUtilities2;

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

        label = new ShadowLabel(parameter.getLabel());
        label.setToolTipText(parameter.getName());

        this.control = control;
        control.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        popupButton = new JButton();
        popupButton.putClientProperty("JButton.buttonType", "roundRect");
        popupButton.putClientProperty("JComponent.sizeVariant", "mini");
        popupButton.setFocusable(false);
        popupButton.setIcon(new Icons.ArrowIcon(Icons.ArrowIcon.SOUTH, new Color(180, 180, 180)));
        JPopupMenu menu = new JPopupMenu();
        expressionMenuItem = new JCheckBoxMenuItem(new ToggleExpressionAction());
        menu.add(expressionMenuItem);
        menu.add(new RevertToDefaultAction());
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
                int labelWidth = 100;
                // Draw border on the side of the label
                g.setColor(new Color(140, 140, 140));
                g.fillRect(x, y + height - 2, labelWidth - 2, 1);
                g.setColor(new Color(166, 166, 166));
                g.fillRect(x, y + height - 1, labelWidth - 2, 1);
                // Draw border on parameter side
                g.setColor(new Color(179, 179, 179));
                g.fillRect(x + labelWidth + 1, y + height - 2, width - labelWidth - 1, 1);
                g.setColor(new Color(213, 213, 213));
                g.fillRect(x + labelWidth + 1, y + height - 1, width - labelWidth - 1, 1);
                g.setColor(borderColor);
            }

            public Insets getBorderInsets(Component c) {
                return new Insets(5, 0, 7, 0);
            }

            public boolean isBorderOpaque() {
                return true;
            }
        });
    }

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

    private class ExpressionFieldChangedAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            try {
                parameter.setExpression(expressionField.getText());
            } catch (ConnectionError ce) {
                JOptionPane.showMessageDialog(ParameterRow.this, ce.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class ShadowLabel extends JLabel {
        public ShadowLabel(String text) {
            super(text);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            FontMetrics fm = SwingUtilities2.getFontMetrics(this, g);
            int textX = getWidth() - fm.stringWidth(getText()) - 10;
            int textY = fm.getAscent();
            g.setColor(SwingUtils.COLOR_NORMAL);
            g.setFont(SwingUtils.FONT_BOLD);
            SwingUtils.drawShadowText(g2, getText(), textX, textY, new Color(176, 176, 176));
        }
    }

}
