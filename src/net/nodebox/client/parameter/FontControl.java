package net.nodebox.client.parameter;

import net.nodebox.client.PlatformUtils;
import net.nodebox.node.Parameter;
import net.nodebox.node.ParameterDataListener;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Provides a control for fonts.
 * <p/>
 * Note that we use the java.awt.Font object as the model object. The displayed name is fontName.
 */
public class FontControl extends JComponent implements ParameterControl, ActionListener, ParameterDataListener {

    private Parameter parameter;
    private JComboBox fontChooser;
    private FontDataModel fontModel;
    private FontCellRenderer fontCellRenderer;

    public FontControl(Parameter parameter) {
        this.parameter = parameter;
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        fontChooser = new JComboBox();
        fontModel = new FontDataModel();
        fontCellRenderer = new FontCellRenderer();
        fontChooser.setModel(fontModel);
        fontChooser.setRenderer(fontCellRenderer);
        fontChooser.putClientProperty("Jcomponent.sizeVariant", "small");
        fontChooser.setPreferredSize(new Dimension(150, 22));
        fontChooser.putClientProperty("JComboBox.isPopDown", Boolean.TRUE);
        fontChooser.addActionListener(this);
        add(fontChooser);
        setValueForControl(parameter.getValue());
        parameter.addDataListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setValueForControl(Object v) {
        String fontName = (String) v;
        fontChooser.setSelectedItem(fontModel.getFont(fontName));
        fontChooser.repaint();
    }

    public void actionPerformed(ActionEvent e) {
        Font font = (Font) fontChooser.getSelectedItem();
        if (font == null) return;
        parameter.setValue(font.getFontName());
    }

    public void valueChanged(Parameter source, Object newValue) {
        setValueForControl(newValue);
    }

    private class FontDataModel implements ComboBoxModel {

        private Font[] fonts;
        private Font selectedFont;

        public FontDataModel() {
            fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        }

        public Font getFont(String fontName) {
            for (Font f : fonts) {
                if (f.getFontName().equals(fontName))
                    return f;
            }
            return null;
        }

        public void setSelectedItem(Object anItem) {
            selectedFont = (Font) anItem;
        }

        public Object getSelectedItem() {
            return selectedFont;
        }

        public int getSize() {
            return fonts.length;
        }

        public Object getElementAt(int index) {
            return fonts[index];
        }

        public void addListDataListener(ListDataListener l) {
            // Listeners are not supported.
        }

        public void removeListDataListener(ListDataListener l) {
            // Listeners are not supported.
        }
    }

    private class FontCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (label == null) return null;
            Font f = (Font) value;
            if (f == null) return label;
            label.setText(f.getFontName());
            label.setFont(PlatformUtils.getSmallFont());
            return label;
        }
    }
}
