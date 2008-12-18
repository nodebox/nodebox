package net.nodebox.client;

import net.nodebox.node.NodeManager;
import net.nodebox.node.NodeType;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class NodeManagerDialog extends JDialog {

    private class NodeListModel implements ListModel {

        private NodeManager nodeManager;
        private java.util.List<NodeType> nodeTypes;

        private NodeListModel(NodeManager nodeManager) {
            this.nodeManager = nodeManager;
            this.nodeTypes = nodeManager.getNodeTypes();
        }

        public int getSize() {
            return nodeTypes.size();
        }

        public Object getElementAt(int index) {
            return nodeTypes.get(index);
        }

        public void addListDataListener(ListDataListener l) {
            // The list is immutable; don't listen.
        }

        public void removeListDataListener(ListDataListener l) {
            // The list is immutable; don't listen.
        }
    }

    private NodeManager nodeManager;
    private JTextField searchField;
    private JList nodeList;
    private NodeType selectedNodeType;
    private DoubleClickListener doubleClickListener = new DoubleClickListener();
    private EscapeListener escapeListener = new EscapeListener();
    private ArrowKeysListener arrowKeysListener = new ArrowKeysListener();

    public NodeManagerDialog(NodeManager nodeManager) {
        this(null, nodeManager);
    }

    public NodeManagerDialog(Frame owner, NodeManager nodeManager) {
        super(owner, "Create node type", true);
        JPanel panel = new JPanel(new BorderLayout());
        this.nodeManager = nodeManager;
        searchField = new JTextField();
        searchField.addKeyListener(arrowKeysListener);
        nodeList = new JList(new NodeListModel(nodeManager));
        nodeList.addMouseListener(doubleClickListener);
        nodeList.addKeyListener(arrowKeysListener);
        nodeList.setSelectedIndex(0);
        panel.add(searchField, BorderLayout.NORTH);
        panel.add(nodeList, BorderLayout.CENTER);
        addEscapeSupport(this);
        setContentPane(panel);
        setSize(346, 382);
        SwingUtils.centerOnScreen(this);
    }

    public NodeType getSelectedNodeType() {
        return selectedNodeType;
    }

    private void addEscapeSupport(Component c) {
        c.addKeyListener(escapeListener);
        if (c instanceof Container) {
            Container container = (Container) c;
            for (Component child : container.getComponents()) {
                addEscapeSupport(child);
            }
        }
    }

    public NodeManager getNodeManager() {
        return nodeManager;
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
}
