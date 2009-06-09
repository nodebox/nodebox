package nodebox.client.parameter;

import nodebox.client.PlatformUtils;
import nodebox.node.Parameter;
import nodebox.node.ParameterValueListener;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuControl extends JComponent implements ParameterControl, ActionListener, ParameterValueListener {

    private Parameter parameter;
    private String value;
    private JComboBox menuBox;
    private MenuDataModel menuModel;

    public MenuControl(Parameter parameter) {
        this.parameter = parameter;
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        menuBox = new JComboBox();
        menuModel = new MenuDataModel(parameter);
        MenuItemRenderer menuItemRenderer = new MenuItemRenderer();
        menuBox.setModel(menuModel);
        menuBox.setRenderer(menuItemRenderer);
        menuBox.putClientProperty("Jcomponent.sizeVariant", "small");
        menuBox.setPreferredSize(new Dimension(150, 22));
        menuBox.putClientProperty("JComboBox.isPopDown", Boolean.TRUE);
        menuBox.addActionListener(this);
        add(menuBox);
        setValueForControl(parameter.getValue());
        parameter.getNode().addParameterValueListener(this);
    }

    public Parameter getParameter() {
        return parameter;
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
        Parameter.MenuItem item = (Parameter.MenuItem) menuBox.getSelectedItem();
        parameter.setValue(item.getKey());
    }

    public void valueChanged(Parameter source) {
        Object newValue = source.getValue();
        if (value != null && value.equals(newValue)) return;
        setValueForControl(newValue);
    }

    private class MenuDataModel implements ComboBoxModel {

        java.util.List<Parameter.MenuItem> menuItems;
        Parameter.MenuItem selectedItem;

        public MenuDataModel(Parameter parameter) {
            menuItems = parameter.getMenuItems();
        }

        public Parameter.MenuItem getMenuItem(String key) {
            for (Parameter.MenuItem item : menuItems) {
                if (item.getKey().equals(key))
                    return item;
            }
            return null;
        }

        public void setSelectedItem(Object anItem) {
            selectedItem = (Parameter.MenuItem) anItem;
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
            Parameter.MenuItem item = (Parameter.MenuItem) value;
            if (item == null) return label;
            label.setText(item.getLabel());
            label.setFont(PlatformUtils.getSmallFont());
            return label;
        }
    }
}
