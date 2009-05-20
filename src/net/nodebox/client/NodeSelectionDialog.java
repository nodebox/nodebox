package net.nodebox.client;

import net.nodebox.node.Node;
import net.nodebox.node.NodeLibraryManager;

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

public class NodeSelectionDialog extends JDialog {

    private class FilteredNodeListModel implements ListModel {

        private NodeLibraryManager manager;
        private java.util.List<Node> filteredNodes;
        private String searchString;

        private FilteredNodeListModel(NodeLibraryManager manager) {
            this.manager = manager;
            this.searchString = "";
            this.filteredNodes = manager.getNodes();
        }

        public String getSearchString() {
            return searchString;
        }

        public void setSearchString(String searchString) {
            this.searchString = searchString.trim();
            if (searchString.length() == 0) {
                filteredNodes = manager.getNodes();

            } else {
                filteredNodes = new ArrayList<Node>();
                for (Node type : manager.getNodes()) {
                    String description = type.getDescription() == null ? "" : type.getDescription();
                    if (type.getName().contains(searchString) ||
                            description.contains(searchString)) {
                        filteredNodes.add(type);
                    }
                }
            }
        }

        public int getSize() {
            return filteredNodes.size();
        }

        public Object getElementAt(int index) {
            return filteredNodes.get(index);
        }

        public void addListDataListener(ListDataListener l) {
            // The list is immutable; don't listen.
        }

        public void removeListDataListener(ListDataListener l) {
            // The list is immutable; don't listen.
        }
    }

    private class NodeRenderer extends JLabel implements ListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            assert (value instanceof Node);
            Node node = (Node) value;
            String html = "<html>" + node.getName() + "<font color=#666666> Ð " + node.getDescription() + "</font></html>";
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

    private NodeLibraryManager manager;
    private JTextField searchField;
    private JList nodeList;
    private Node selectedNode;
    private FilteredNodeListModel filteredNodeListModel;

    public NodeSelectionDialog(NodeLibraryManager manager) {
        this(null, manager);
    }

    public NodeSelectionDialog(Frame owner, NodeLibraryManager manager) {
        super(owner, "Create node type", true);
        getRootPane().putClientProperty("Window.style", "small");
        JPanel panel = new JPanel(new BorderLayout());
        this.manager = manager;
        filteredNodeListModel = new FilteredNodeListModel(manager);
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.variant", "search");
        EscapeListener escapeListener = new EscapeListener();
        searchField.addKeyListener(escapeListener);
        ArrowKeysListener arrowKeysListener = new ArrowKeysListener();
        searchField.addKeyListener(arrowKeysListener);
        SearchFieldChangeListener searchFieldChangeListener = new SearchFieldChangeListener();
        searchField.getDocument().addDocumentListener(searchFieldChangeListener);
        nodeList = new JList(filteredNodeListModel);
        DoubleClickListener doubleClickListener = new DoubleClickListener();
        nodeList.addMouseListener(doubleClickListener);
        nodeList.addKeyListener(escapeListener);
        nodeList.addKeyListener(arrowKeysListener);
        nodeList.setSelectedIndex(0);
        nodeList.setCellRenderer(new NodeRenderer());
        panel.add(searchField, BorderLayout.NORTH);
        panel.add(nodeList, BorderLayout.CENTER);
        setContentPane(panel);
        setSize(346, 382);
        SwingUtils.centerOnScreen(this);
    }

    public Node getSelectedNode() {
        return selectedNode;
    }

    public NodeLibraryManager getManager() {
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
        selectedNode = (Node) nodeList.getSelectedValue();
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
