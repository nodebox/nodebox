package nodebox.client;

import nodebox.node.Node;
import nodebox.node.NodeLibrary;
import nodebox.node.NodeLibraryManager;
import nodebox.util.StringUtils;

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
import java.util.Collections;
import java.util.Comparator;

public class NodeSelectionDialog extends JDialog {

    private class FilteredNodeListModel implements ListModel {

        private NodeLibrary library;
        private NodeLibraryManager manager;
        private java.util.List<Node> filteredNodes;
        private String searchString;

        private FilteredNodeListModel(NodeLibrary library, NodeLibraryManager manager) {
            this.library = library;
            this.manager = manager;
            searchString = "";
            filteredNodes = manager.getNodes();
            filteredNodes.addAll(library.getExportedNodes());
            Collections.sort(filteredNodes, new NodeNameComparator());
        }

        public String getSearchString() {
            return searchString;
        }

        public void setSearchString(String searchString) {
            this.searchString = searchString = searchString.trim().toLowerCase();
            if (searchString.length() == 0) {
                // Add all the nodes from the manager.
                filteredNodes = manager.getNodes();
                // Add all the exported nodes from the current library.
                filteredNodes.addAll(library.getExportedNodes());
                Collections.sort(filteredNodes, new NodeNameComparator());
            } else {
                java.util.List<Node> nodes = new ArrayList<Node>();

                filteredNodes = new ArrayList<Node>();
                // Add all the nodes from the manager.
                for (Node node : manager.getNodes()) {
                    if (contains(node, searchString))
                        nodes.add(node);
                }
                // Add all the exported nodes from the current library.
                for (Node node : library.getExportedNodes()) {
                    if (contains(node, searchString))
                        nodes.add(node);
                }

                filteredNodes = sortNodes(nodes, this.searchString);
            }
        }

        private boolean contains(Node node, String searchString) {
            String description = node.getDescription() == null ? "" : node.getDescription().toLowerCase();
            return node.getName().toLowerCase().contains(searchString) || description.contains(searchString);
        }


        private java.util.List<Node> sortNodes(java.util.List<Node> nodes, String searchString) {
            java.util.List<Node> sortedNodes = new ArrayList<Node>();
            java.util.List<Node> startsWithNodes = new ArrayList<Node>();
            java.util.List<Node> containsNodes = new ArrayList<Node>();
            java.util.List<Node> descriptionNodes = new ArrayList<Node>();

            for (Node node : nodes) {
                if (node.getName().equals(searchString))
                    sortedNodes.add(node);
                else if (node.getName().startsWith(searchString))
                    startsWithNodes.add(node);
                else if (node.getName().contains(searchString))
                    containsNodes.add(node);
                else
                    descriptionNodes.add(node);
            }
            Collections.sort(startsWithNodes, new NodeNameComparator());
            sortedNodes.addAll(startsWithNodes);
            Collections.sort(containsNodes, new NodeNameComparator());
            sortedNodes.addAll(containsNodes);
            Collections.sort(descriptionNodes, new NodeNameComparator());
            sortedNodes.addAll(descriptionNodes);
            return sortedNodes;
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
            String html = "<html><b>" + StringUtils.humanizeName(node.getName()) + "</b> - " + node.getDescription() + "<br><font color=#333333>" + node.getDataClass().getSimpleName() + " - </font> <font color=#666666>" + node.getLibrary().getName() + "</font></html>";
            setText(html);
            if (isSelected) {
                setBackground(Theme.NODE_SELECTION_ACTIVE_BACKGROUND_COLOR);
            } else {
                setBackground(Theme.NODE_SELECTION_BACKGROUND_COLOR);
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setIcon(new ImageIcon(NodeView.getImageForNode(node)));
            setBorder(Theme.BOTTOM_BORDER);
            setOpaque(true);
            return this;
        }
    }

    private NodeLibrary library;
    private NodeLibraryManager manager;
    private JTextField searchField;
    private JList nodeList;
    private Node selectedNode;
    private FilteredNodeListModel filteredNodeListModel;

    public NodeSelectionDialog(NodeLibrary library, NodeLibraryManager manager) {
        this(null, library, manager);
    }

    public NodeSelectionDialog(Frame owner, NodeLibrary library, NodeLibraryManager manager) {
        super(owner, "New Node", true);
        getRootPane().putClientProperty("Window.style", "small");
        JPanel panel = new JPanel(new BorderLayout());
        this.library = library;
        this.manager = manager;
        filteredNodeListModel = new FilteredNodeListModel(library, manager);
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
        JScrollPane nodeScroll = new JScrollPane(nodeList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        nodeScroll.setBorder(null);
        panel.add(searchField, BorderLayout.NORTH);
        panel.add(nodeScroll, BorderLayout.CENTER);
        setContentPane(panel);
        setSize(500, 400);
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
                if (e.getSource() == searchField)
                    moveUp();
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                if (e.getSource() == searchField)
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
            nodeList.ensureIndexIsVisible(0);
            repaint();
        }
    }

    private class NodeNameComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Node node1 = (Node) o1;
            Node node2 = (Node) o2;
            return node1.getName().compareTo(node2.getName());
        }
    }
}
