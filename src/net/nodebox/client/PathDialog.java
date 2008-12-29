package net.nodebox.client;

import net.nodebox.node.Network;
import net.nodebox.node.Node;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PathDialog extends JDialog {

    public static String choosePath(Network network, String path) {
        PathDialog dialog = new PathDialog(network);
        dialog.setModal(true);
        dialog.setVisible(true);
        return dialog.getSelectedPath();
    }

    private Network network;
    private JTree nodeTree;
    private NetworkTreeModel networkTreeModel;
    private NodeCellRenderer nodeCellRenderer;

    /**
     * The given network is the root. If you want to limit the selection to only part of the tree,
     * don't give the root network but provide the network you want to use as root for the tree.
     *
     * @param network the root network for this tree.
     */
    public PathDialog(Network network) {
        setTitle("Select node");
        //JPanel panel = new JPanel(new BorderLayout(0, 0));
        networkTreeModel = new NetworkTreeModel(network);
        nodeTree = new JTree(networkTreeModel);
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

    private class NetworkTreeModel implements TreeModel {
        private Network rootNetwork;

        private NetworkTreeModel(Network rootNetwork) {
            this.rootNetwork = rootNetwork;
        }

        public Object getRoot() {
            return rootNetwork;
        }

        public Object getChild(Object parent, int index) {
            Network net = (Network) parent;
            return net.getNodes().toArray()[index];
        }

        public int getChildCount(Object parent) {
            Network net = (Network) parent;
            return net.getNodes().size();
        }

        public boolean isLeaf(Object node) {
            return !(node instanceof Network);
        }

        public void valueForPathChanged(TreePath path, Object newValue) {
            // Not implemented: the tree model is immutable.
        }

        public int getIndexOfChild(Object parent, Object child) {
            Network net = (Network) parent;
            int index = 0;
            for (Node n : net.getNodes()) {
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
            setFont(PlatformUtils.getSmallFont());
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
