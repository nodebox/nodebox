package net.nodebox.client.parameter;

import net.nodebox.client.SwingUtils;
import net.nodebox.node.Parameter;
import net.nodebox.node.ParameterDataListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ColorControl extends JComponent implements ChangeListener, ParameterControl, ParameterDataListener, ActionListener {

    private Parameter parameter;
    //private ColorWell colorWell;
    private ColorButton colorButton;

    public ColorControl(Parameter parameter) {
        setLayout(new FlowLayout(FlowLayout.LEADING));
        this.parameter = parameter;

//        colorWell = new ColorWell();
//        colorWell.setColor(parameter.asColor());
//        colorWell.addChangeListener(this);
//        add(colorWell);
        colorButton = new ColorButton();
        add(colorButton);
        setValueForControl(parameter.getValue());
        parameter.addDataListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void stateChanged(ChangeEvent e) {
        setValueFromControl();
    }

    private void setValueFromControl() {
        //parameter.setValue(colorWell.getColor());
    }

    public void setValueForControl(Object v) {
        colorButton.repaint();
        //colorWell.setColor((net.nodebox.graphics.Color) v);
    }

    public void valueChanged(Parameter source, Object newValue) {
        setValueForControl(newValue);
    }

    public void actionPerformed(ActionEvent e) {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        final JDialog d = new JDialog(frame, "Choose Color", true);
        d.getContentPane().setLayout(new BorderLayout(10, 10));
        final ColorSwatch colorSwatch = new ColorSwatch(parameter.asColor());
        final JColorChooser colorChooser = new JColorChooser(parameter.asColor().getAwtColor());
        //colorChooser.setPreviewPanel(colorSwatch);
        colorChooser.setPreviewPanel(new JPanel());
        JPanel alphaPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 0));
        JLabel alphaLabel = new JLabel("Alpha:");
        final JSlider alphaSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) (parameter.asColor().getAlpha() * 100));
        alphaPanel.add(alphaLabel);
        alphaPanel.add(alphaSlider);
        JPanel colorPanel = new JPanel(new BorderLayout(10, 10));
        colorPanel.add(colorSwatch, BorderLayout.NORTH);
        colorPanel.add(colorChooser, BorderLayout.CENTER);
        colorPanel.add(alphaPanel, BorderLayout.SOUTH);
        d.getContentPane().add(colorPanel, BorderLayout.CENTER);
        JButton btn = new JButton("OK");
        btn.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                d.setVisible(false);
            }
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        buttonPanel.add(btn);
        d.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Color awtColor = colorChooser.getColor();
                colorSwatch.setColor(new net.nodebox.graphics.Color(
                        awtColor.getRed(),
                        awtColor.getGreen(),
                        awtColor.getBlue(),
                        alphaSlider.getValue() / 100.0));
                colorSwatch.repaint();
            }
        };

        colorChooser.getSelectionModel().addChangeListener(changeListener);
        alphaSlider.getModel().addChangeListener(changeListener);

        d.pack();
        //d.setSize(400, 400);
        SwingUtils.centerOnScreen(d);
        // This goes into modal loop.
        d.setVisible(true);
        Color awtColor = colorChooser.getColor();
        net.nodebox.graphics.Color c = new net.nodebox.graphics.Color(
                awtColor.getRed(),
                awtColor.getGreen(),
                awtColor.getBlue(),
                alphaSlider.getValue() / 100.0);
        parameter.setValue(c);
    }

    private class ColorSwatch extends JComponent {
        private net.nodebox.graphics.Color color = new net.nodebox.graphics.Color();

        private ColorSwatch(net.nodebox.graphics.Color color) {
            setSize(30, 30);
            setBounds(0, 0, 30, 30);
            setPreferredSize(new Dimension(30, 30));
            this.color = color;
        }

        public net.nodebox.graphics.Color getColor() {
            return color;
        }

        public void setColor(net.nodebox.graphics.Color color) {
            this.color = color;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Rectangle r = g.getClipBounds();
            g.setColor(color.getAwtColor());
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }

    private class ColorButton extends JButton {
        private ColorButton() {
            addActionListener(ColorControl.this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Rectangle r = g.getClipBounds();
            g.setColor(parameter.asColor().getAwtColor());
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }
}
