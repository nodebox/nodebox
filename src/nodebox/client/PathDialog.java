package nodebox.client;

import nodebox.node.Node;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PathDialog extends JDialog {

    public static String choosePath(Node root, String path) {
        PathDialog dialog = new PathDialog(root);
        dialog.setModal(true);
        dialog.setVisible(true);
        return dialog.getSelectedPath();
    }

    private Node root;
    private JTree nodeTree;
    private NodeTreeModel nodeTreeModel;
    private NodeCellRenderer nodeCellRenderer;

    /**
     * The given network is the root. If you want to limit the selection to only part of the tree,
     * don't give the root network but provide the network you want to use as root for the tree.
     *
     * @param root the root network for this tree.
     */
    public PathDialog(Node root) {
        setTitle("Select node");
        //JPanel panel = new JPanel(new BorderLayout(0, 0));
        nodeTreeModel = new NodeTreeModel(root);
        nodeTree = new JTree(nodeTreeModel);
        nodeTree.addMouseListener(new DoubleClickHandler());
        //nodeTree.setSelectionModel(new DefaultTR());
        nodeCellRenderer = new NodeCellRenderer();
        nodeTree.setCellRenderer(nodeCellRenderer);
        //panel.add(nodeTree, BorderLayout.CENTER);
        JScrollPane scrollPane = new JScrollPane(nodeTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setContentPane(scrollPane);
        setSize(300, 400);
        SwingUtils.centerOnScreen(this);
    }

    public String getSelectedPath() {
        Node n = getSelectedNode();
        if (n == null)
            return null;
        return n.getAbsolutePath();
    }

    public Node getSelectedNode() {
        return (Node) nodeTree.getSelectionPath().getLastPathComponent();
    }

    public void setSelectedNode(Node node) {

    }

    public void setSelectedPath(String path) {

    }

    private class NodeTreeModel implements TreeModel {
        private Node root;

        private NodeTreeModel(Node root) {
            this.root = root;
        }

        public Object getRoot() {
            return root;
        }

        public Object getChild(Object parent, int index) {
            Node parentNode = (Node) parent;
            return parentNode.getChildAt(index);
        }

        public int getChildCount(Object parent) {
            Node parentNode = (Node) parent;
            return parentNode.getChildCount();
        }

        public boolean isLeaf(Object node) {
            Node parentNode = (Node) node;
            return !parentNode.hasChildren();
        }

        public void valueForPathChanged(TreePath path, Object newValue) {
            // Not implemented: the tree model is immutable.
        }

        public int getIndexOfChild(Object parent, Object child) {
            Node parentNode = (Node) parent;
            int index = 0;
            for (Node n : parentNode.getChildren()) {
                if (n == child)
                    return index;
                index++;
            }
            return -1;
        }

        public void addTreeModelListener(TreeModelListener l) {
            // Listeners not implemented.
        }

        public void removeTreeModelListener(TreeModelListener l) {
            // Listeners not implemented.
        }
    }

    private class DoubleClickHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                setVisible(false);
            }
        }
    }

    private class NodeCellRenderer extends DefaultTreeCellRenderer {

        private NodeCellRenderer() {
            setFont(Theme.SMALL_FONT);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (label == null) return null;
            Node node = (Node) value;
            label.setText(node.getName());
            return label;
        }
    }
}
