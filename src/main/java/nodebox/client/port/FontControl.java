package nodebox.client.port;

import nodebox.node.Port;
import nodebox.ui.Theme;

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
public class FontControl extends AbstractPortControl implements ActionListener {

    private JComboBox<Font> fontChooser;
    private FontDataModel fontModel;
    private String value;

    public FontControl(String nodePath, Port port) {
        super(nodePath, port);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        fontChooser = new JComboBox<>();
        fontModel = new FontDataModel();
        FontCellRenderer fontCellRenderer = new FontCellRenderer();
        fontChooser.setModel(fontModel);
        fontChooser.setRenderer(fontCellRenderer);
        fontChooser.putClientProperty("JComponent.sizeVariant", "small");
        fontChooser.setPreferredSize(new Dimension(150, 22));
        fontChooser.putClientProperty("JComboBox.isPopDown", Boolean.TRUE);
        fontChooser.addActionListener(this);
        fontChooser.setFont(Theme.SMALL_BOLD_FONT);
        add(fontChooser);
        setValueForControl(port.getValue());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        fontChooser.setEnabled(enabled);
    }

    public void setValueForControl(Object v) {
        String fontName = (String) v;
        if (value != null && value.equals(fontName)) return;
        fontChooser.setSelectedItem(fontModel.getFont(fontName));
        fontChooser.repaint();
        value = fontName;
    }

    public void actionPerformed(ActionEvent e) {
        Font font = (Font) fontChooser.getSelectedItem();
        if (font == null) return;
        setPortValue(font.getFontName());
    }

    private class FontDataModel implements ComboBoxModel<Font> {

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

        public Font getElementAt(int index) {
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
            label.setFont(Theme.SMALL_BOLD_FONT);
            return label;
        }
    }
}
