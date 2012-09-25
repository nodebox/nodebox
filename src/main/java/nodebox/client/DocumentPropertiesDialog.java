package nodebox.client;

import nodebox.node.Node;
import nodebox.node.NodeLibrary;
import nodebox.ui.Borders;
import nodebox.ui.MessageBar;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class DocumentPropertiesDialog extends JDialog {

    private final NodeBoxDocument document;

    private JTable table;
    private HashMap<String, String> properties;
    private ArrayList<String> propertyKeys;
    private int propertyCounter = 1;
    private final TableModel propertiesModel;
    private boolean committed = false;

    public DocumentPropertiesDialog(NodeBoxDocument document) {
        super(document, "Document Properties", true);
        this.document = document;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));

        mainPanel.add(new MessageBar("<html>&nbsp;&nbsp;&nbsp;Properties are document metadata that can be used in external programs.</html>", MessageBar.Type.INFO), BorderLayout.NORTH);

        propertiesModel = new PropertiesModel();
        table = new JTable(propertiesModel);
        table.setShowGrid(true);
        table.setBorder(null);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(null);

        properties = new HashMap<String, String>(document.getNodeLibrary().getProperties());
        propertyKeys = new ArrayList<String>(properties.keySet());
        Collections.sort(propertyKeys);
        mainPanel.add(tableScroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String key = "property" + propertyCounter;
                properties.put(key, "value");
                propertyKeys.add(key);
                propertyCounter++;
                table.tableChanged(new TableModelEvent(propertiesModel));
            }
        });
        buttonPanel.add(addButton);


        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int rowIndex = table.getSelectedRow();
                if (rowIndex >= 0) {
                    String key = propertyKeys.get(rowIndex);
                    propertyKeys.remove(rowIndex);
                    properties.remove(key);
                    table.tableChanged(new TableModelEvent(propertiesModel));
                }
            }
        });
        buttonPanel.add(removeButton);

        buttonPanel.add(Box.createGlue());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                committed = false;
                setVisible(false);
            }
        });
        buttonPanel.add(cancelButton);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                committed = true;
                setVisible(false);
            }
        });
        buttonPanel.add(saveButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setSize(500, 400);
        setMinimumSize(new Dimension(500, 300));
        setLocationRelativeTo(document);
    }

    public boolean isCommitted() {
        return committed;
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    private class PropertiesModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return propertyKeys.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return "Property";
            } else {
                return "Value";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object o, int rowIndex, int columnIndex) {
            String key = propertyKeys.get(rowIndex);
            if (columnIndex == 0) {
                String newKey = (String) o;
                propertyKeys.set(rowIndex, newKey);
                String oldValue = properties.remove(key);
                properties.put(newKey, oldValue);
            } else {
                String newValue = (String) o;
                properties.put(key, newValue);
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String key = propertyKeys.get(rowIndex);
            if (columnIndex == 0) {
                return key;
            } else {
                return properties.get(key);
            }
        }


    }

    public static void main(String[] args) {
        NodeLibrary library = NodeLibrary.create("test", Node.ROOT);
        library = library.withProperty("screenshot.region", "network");
        library = library.withProperty("screenshot.path", "/root/invader");
        DocumentPropertiesDialog editor = new DocumentPropertiesDialog(new NodeBoxDocument(library));
        editor.setVisible(true);
    }

}
