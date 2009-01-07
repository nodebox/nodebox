package net.nodebox.client;

import net.nodebox.node.NodeType;
import net.nodebox.node.NodeTypeLibraryManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class NodeTypeDialog extends JDialog {

    private class FilteredNodeListModel implements ListModel {

        private NodeTypeLibraryManager manager;
        private java.util.List<NodeType> filteredNodeTypes;
        private String searchString;

        private FilteredNodeListModel(NodeTypeLibraryManager manager) {
            this.manager = manager;
            this.searchString = "";
            this.filteredNodeTypes = manager.getNodeTypes();
        }

        public String getSearchString() {
            return searchString;
        }

        public void setSearchString(String searchString) {
            this.searchString = searchString.trim();
            if (searchString.length() == 0) {
                filteredNodeTypes = manager.getNodeTypes();

            } else {
                filteredNodeTypes = new ArrayList<NodeType>();
                for (NodeType type : manager.getNodeTypes()) {
                    String description = type.getDescription() == null ? "" : type.getDescription();
                    if (type.getName().contains(searchString) ||
                            description.contains(searchString)) {
                        filteredNodeTypes.add(type);
                    }
                }
            }
        }

        public int getSize() {
            return filteredNodeTypes.size();
        }

        public Object getElementAt(int index) {
            return filteredNodeTypes.get(index);
        }

        public void addListDataListener(ListDataListener l) {
            // The list is immutable; don't listen.
        }

        public void removeListDataListener(ListDataListener l) {
            // The list is immutable; don't listen.
        }
    }

    private class NodeTypeRenderer extends JLabel implements ListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            assert (value instanceof NodeType);
            NodeType nodeType = (NodeType) value;
            String html = "<html>" + nodeType.getName() + "<font color=#666666> Ð " + nodeType.getDescription() + "</font></html>";
            setText(html);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setOpaque(true);
            return this;
        }

    }

    private NodeTypeLibraryManager manager;
    private JTextField searchField;
    private JList nodeList;
    private NodeType selectedNodeType;
    private DoubleClickListener doubleClickListener = new DoubleClickListener();
    private EscapeListener escapeListener = new EscapeListener();
    private ArrowKeysListener arrowKeysListener = new ArrowKeysListener();
    private SearchFieldChangeListener searchFieldChangeListener = new SearchFieldChangeListener();
    private FilteredNodeListModel filteredNodeListModel;

    public NodeTypeDialog(NodeTypeLibraryManager manager) {
        this(null, manager);
    }

    public NodeTypeDialog(Frame owner, NodeTypeLibraryManager manager) {
        super(owner, "Create node type", true);
        getRootPane().putClientProperty("Window.style", "small");
        JPanel panel = new JPanel(new BorderLayout());
        this.manager = manager;
        filteredNodeListModel = new FilteredNodeListModel(manager);
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.variant", "search");
        searchField.addKeyListener(escapeListener);
        searchField.addKeyListener(arrowKeysListener);
        searchField.getDocument().addDocumentListener(searchFieldChangeListener);
        nodeList = new JList(filteredNodeListModel);
        nodeList.addMouseListener(doubleClickListener);
        nodeList.addKeyListener(escapeListener);
        nodeList.addKeyListener(arrowKeysListener);
        nodeList.setSelectedIndex(0);
        nodeList.setCellRenderer(new NodeTypeRenderer());
        panel.add(searchField, BorderLayout.NORTH);
        panel.add(nodeList, BorderLayout.CENTER);
        setContentPane(panel);
        setSize(346, 382);
        SwingUtils.centerOnScreen(this);
    }

    public NodeType getSelectedNodeType() {
        return selectedNodeType;
    }

    public NodeTypeLibraryManager getManager() {
        return manager;
    }

    private void closeDialog() {
        setVisible(false);
    }

    private void moveUp() {
        int index = nodeList.getSelectedIndex();
        index--;
        if (index < 0) {
            index = nodeList.getModel().getSize() - 1;
        }
        nodeList.setSelectedIndex(index);
    }

    private void moveDown() {
        int index = nodeList.getSelectedIndex();
        index++;
        if (index >= nodeList.getModel().getSize()) {
            index = 0;
        }
        nodeList.setSelectedIndex(index);
    }

    private void selectAndClose() {
        selectedNodeType = (NodeType) nodeList.getSelectedValue();
        closeDialog();
    }

    private class DoubleClickListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                selectAndClose();
            }
        }
    }

    private class EscapeListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                closeDialog();
        }
    }


    private class ArrowKeysListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_UP) {
                moveUp();
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                moveDown();
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                selectAndClose();
            }
        }
    }

    private class SearchFieldChangeListener implements DocumentListener {
        public void insertUpdate(DocumentEvent e) {
            changedEvent();
        }

        public void removeUpdate(DocumentEvent e) {
            changedEvent();
        }

        public void changedUpdate(DocumentEvent e) {
            changedEvent();
        }

        private void changedEvent() {
            if (filteredNodeListModel.getSearchString().equals(searchField.getText())) return;
            filteredNodeListModel.setSearchString(searchField.getText());
            // Trigger a model reload.
            nodeList.setModel(filteredNodeListModel);
            nodeList.setSelectedIndex(0);
            repaint();
        }
    }
}
