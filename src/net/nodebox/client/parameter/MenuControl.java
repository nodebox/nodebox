package net.nodebox.client.parameter;

import net.nodebox.client.PlatformUtils;
import net.nodebox.node.Parameter;
import net.nodebox.node.ParameterDataListener;
import net.nodebox.node.ParameterType;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuControl extends JComponent implements ParameterControl, ActionListener, ParameterDataListener {

    private Parameter parameter;
    private JComboBox menuBox;
    private MenuDataModel menuModel;
    private MenuItemRenderer menuItemRenderer;

    public MenuControl(Parameter parameter) {
        this.parameter = parameter;
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        menuBox = new JComboBox();
        menuModel = new MenuDataModel(parameter.getParameterType());
        menuItemRenderer = new MenuItemRenderer();
        menuBox.setModel(menuModel);
        menuBox.setRenderer(menuItemRenderer);
        menuBox.putClientProperty("Jcomponent.sizeVariant", "small");
        menuBox.setPreferredSize(new Dimension(150, 22));
        menuBox.putClientProperty("JComboBox.isPopDown", Boolean.TRUE);
        menuBox.addActionListener(this);
        add(menuBox);
        setValueForControl(parameter.getValue());
        parameter.addDataListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setValueForControl(Object v) {
        String key = (String) v;
        Object item = menuModel.getMenuItem(key);
        if (menuBox.getSelectedItem() == item) return;
        menuBox.setSelectedItem(item);
        // The menuBox does not update automatically
        menuBox.repaint();
    }

    public void actionPerformed(ActionEvent e) {
        ParameterType.MenuItem item = (ParameterType.MenuItem) menuBox.getSelectedItem();
        parameter.setValue(item.getKey());
    }

    public void valueChanged(Parameter source, Object newValue) {
        setValueForControl(newValue);
    }

    private class MenuDataModel implements ComboBoxModel {

        java.util.List<ParameterType.MenuItem> menuItems;
        ParameterType.MenuItem selectedItem;

        public MenuDataModel(ParameterType pType) {
            menuItems = pType.getMenuItems();
        }

        public ParameterType.MenuItem getMenuItem(String key) {
            for (ParameterType.MenuItem item : menuItems) {
                if (item.getKey().equals(key))
                    return item;
            }
            return null;
        }

        public void setSelectedItem(Object anItem) {
            selectedItem = (ParameterType.MenuItem) anItem;
        }

        public Object getSelectedItem() {
            return selectedItem;
        }

        public int getSize() {
            return menuItems.size();
        }

        public Object getElementAt(int index) {
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
            ParameterType.MenuItem item = (ParameterType.MenuItem) value;
            if (item == null) return label;
            label.setText(item.getLabel());
            label.setFont(PlatformUtils.getSmallFont());
            return label;
        }
    }
}
