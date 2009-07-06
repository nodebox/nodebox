package nodebox.client;

import nodebox.node.ConnectionError;
import nodebox.node.Parameter;
import nodebox.node.ParameterValueListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

public class ParameterRow extends JComponent implements ComponentListener, MouseListener, ParameterValueListener, ActionListener {

    private static Image popupButtonImage;
    private static int popupButtonHeight;
    private static Border rowBorder = new RowBorder();

    static {
        try {
            popupButtonImage = ImageIO.read(new File("res/options-button.png"));
            popupButtonHeight = popupButtonImage.getHeight(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Parameter parameter;
    private JLabel label;
    private JComponent control;
    private JPanel expressionPanel;
    private JTextField expressionField;
    private JPopupMenu popupMenu;
    private JCheckBoxMenuItem expressionMenuItem;

    private static final int TOP_PADDING = 2;
    private static final int BOTTOM_PADDING = 2;
    private ExpressionWindow window;

    public ParameterRow(Parameter parameter, JComponent control) {
        addComponentListener(this);
        addMouseListener(this);
        this.parameter = parameter;

        setLayout(null);

        label = new ShadowLabel(parameter.getLabel());
        label.setToolTipText(parameter.getName());

        this.control = control;
        control.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        //if (control instanceof ContainerListener)
        //    addContainerListener((ContainerListener) control);

        popupMenu = new JPopupMenu();
        expressionMenuItem = new JCheckBoxMenuItem(new ToggleExpressionAction());
        popupMenu.add(expressionMenuItem);
        popupMenu.add(new RevertToDefaultAction());

        expressionPanel = new JPanel(new BorderLayout(5, 0));
        expressionPanel.setOpaque(false);
        expressionPanel.setVisible(false);
        expressionField = new JTextField();
        expressionField.setAction(new ExpressionFieldChangedAction());
        expressionField.putClientProperty("JComponent.sizeVariant", "small");
        expressionField.setFont(PlatformUtils.getSmallBoldFont());
        JButton expressionButton = new JButton("...");
        expressionButton.putClientProperty("JButton.buttonType", "gradient");
        expressionButton.setPreferredSize(new Dimension(30, 27));
        expressionButton.addActionListener(this);
        expressionPanel.add(expressionField, BorderLayout.CENTER);
        expressionPanel.add(expressionButton, BorderLayout.EAST);

        add(this.label);
        add(this.control);
        add(this.expressionPanel);
        componentResized(null);
        setExpressionStatus();

        setBorder(rowBorder);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        parameter.getNode().addParameterValueListener(this);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        parameter.getNode().removeParameterValueListener(this);
    }

    //// Component listeners ////

    public void componentResized(ComponentEvent e) {
        Dimension controlSize = control.getPreferredSize();
        Rectangle bounds = getBounds();
        int h = bounds.height - TOP_PADDING - BOTTOM_PADDING;
        label.setBounds(0, TOP_PADDING, ParameterView.LABEL_WIDTH, h);
        control.setBounds(ParameterView.LABEL_WIDTH + 10, TOP_PADDING, controlSize.width, h);
        control.doLayout();
        expressionPanel.setBounds(ParameterView.LABEL_WIDTH + 10, TOP_PADDING, 200, h);
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

    //// Mouse listeners ////

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.getX() < this.getWidth() - 20) return;
        popupMenu.show(this, this.getWidth() - 20, 20);
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Height aligns to 30px high control, such as float, string, color, etc.
        g.drawImage(popupButtonImage, getWidth() - 20, 4, null);

    }

    //// Parameter context menu ////

    private void setExpressionStatus() {
        // Check if the current state is already correct.
        if (parameter.hasExpression() && !control.isVisible()
                && expressionField.getText().equals(parameter.getExpression())) return;
        if (parameter.hasExpression()) {
            control.setVisible(false);
            expressionPanel.setVisible(true);
            expressionField.setText(parameter.getExpression());

        } else {
            control.setVisible(true);
            expressionPanel.setVisible(false);
        }
        expressionMenuItem.setState(parameter.hasExpression());
    }

    /**
     * Check if the value change triggered a change in expression status.
     * <p/>
     * This can happen if revert to default switches from value to expression
     * or vice versa.
     *
     * @param source the Parameter this event comes from
     */
    public void valueChanged(Parameter source) {
        if (parameter != source) return;
        setExpressionStatus();
    }

    /**
     * A user clicked the expression editor button. Show the expression window.
     *
     * @param e the event
     */
    public void actionPerformed(ActionEvent e) {
        window = new ExpressionWindow(parameter);
        window.setLocationRelativeTo(this);
        window.setVisible(true);
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
            // We don't have to change the expression status here.
            // Instead, we respond to the valueChanged event to update our status.
            // This makes the handling consistent even with multiple parameter views.
        }
    }

    private class RevertToDefaultAction extends AbstractAction {
        private RevertToDefaultAction() {
            putValue(Action.NAME, "Revert to Default");
        }

        public void actionPerformed(ActionEvent e) {
            parameter.revertToDefault();
            // Reverting to default could cause an expression to be set/cleared.
            // This triggers an valueChanged event, where we check if our expression field is
            // still up-to-date.
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

    //// Draw classes ////

    private class ShadowLabel extends JLabel {
        public ShadowLabel(String text) {
            super(text);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(SwingUtils.COLOR_NORMAL);
            g2.setFont(SwingUtils.FONT_BOLD);
            int textX = ParameterView.LABEL_WIDTH - g2.getFontMetrics().stringWidth(getText()) - 10;
            // Add some padding to align it to 30px high components.
            int textY = 19;
            SwingUtils.drawShadowText(g2, getText(), textX, textY, new Color(176, 176, 176), 1);
        }
    }

    private static class RowBorder implements Border {
        private static Color labelUp = new Color(140, 140, 140);
        private static Color labelDown = new Color(166, 166, 166);
        private static Color parameterUp = new Color(179, 179, 179);
        private static Color parameterDown = new Color(213, 213, 213);

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            int labelWidth = ParameterView.LABEL_WIDTH;
            // Draw border on the side of the label
            g.setColor(labelUp);
            g.fillRect(x, y + height - 2, labelWidth - 2, 1);
            g.setColor(labelDown);
            g.fillRect(x, y + height - 1, labelWidth - 2, 1);
            // Draw border on parameter side
            g.setColor(parameterUp);
            g.fillRect(x + labelWidth + 1, y + height - 2, width - labelWidth - 1, 1);
            g.setColor(parameterDown);
            g.fillRect(x + labelWidth + 1, y + height - 1, width - labelWidth - 1, 1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(5, 0, 7, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

}
