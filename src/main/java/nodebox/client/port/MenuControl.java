package nodebox.client.port;

import nodebox.node.MenuItem;
import nodebox.node.Port;
import nodebox.ui.Theme;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class MenuControl extends AbstractPortControl implements ActionListener {

    private String value;
    private JComboBox<MenuItem> menuBox;
    private MenuDataModel menuModel;

    public MenuControl(String nodePath, Port port) {
        super(nodePath, port);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        menuBox = new JComboBox<>();
        menuModel = new MenuDataModel(port);
        MenuItemRenderer menuItemRenderer = new MenuItemRenderer();
        menuBox.setModel(menuModel);
        menuBox.setRenderer(menuItemRenderer);
        menuBox.putClientProperty("JComponent.sizeVariant", "small");
        menuBox.putClientProperty("JComboBox.isPopDown", Boolean.TRUE);
        menuBox.setFont(Theme.SMALL_BOLD_FONT);
        menuBox.addActionListener(this);
        add(menuBox);
        setValueForControl(port.getValue());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        menuBox.setEnabled(enabled);
    }

    public void setValueForControl(Object v) {
        String key = (String) v;
        if (value != null && value.equals(key)) return;
        Object item = menuModel.getMenuItem(key);
        if (menuBox.getSelectedItem() == item) return;
        menuBox.setSelectedItem(item);
        // The menuBox does not update automatically
        menuBox.repaint();
        value = key;
    }

    public void actionPerformed(ActionEvent e) {
        MenuItem item = (MenuItem) menuBox.getSelectedItem();
        if (item != null) {
            setPortValue(item.getKey());
        }
    }

    private class MenuDataModel implements ComboBoxModel<MenuItem> {

        List<MenuItem> menuItems;
        MenuItem selectedItem;

        public MenuDataModel(Port port) {
            menuItems = port.getMenuItems();
        }

        public MenuItem getMenuItem(String key) {
            for (MenuItem item : menuItems) {
                if (item.getKey().equals(key))
                    return item;
            }
            return null;
        }

        public void setSelectedItem(Object anItem) {
            selectedItem = (MenuItem) anItem;
        }

        public Object getSelectedItem() {
            return selectedItem;
        }

        public int getSize() {
            return menuItems.size();
        }

        public MenuItem getElementAt(int index) {
            return menuItems.get(index);
        }

        public void addListDataListener(ListDataListener l) {
            // Listeners are not supported.
        }

        public void removeListDataListener(ListDataListener l) {
            // Listeners are not supported.
        }
    }

    private class MenuItemRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (label == null) return null;
            MenuItem item = (MenuItem) value;
            if (item == null) return label;
            label.setText(item.getLabel());
            label.setFont(Theme.SMALL_BOLD_FONT);
            return label;
        }
    }

}
