package nodebox.client;

import nodebox.Icons;
import nodebox.node.Connection;
import nodebox.node.Node;
import nodebox.node.NodeLibrary;
import nodebox.node.Port;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;

import static nodebox.base.Preconditions.checkState;

public class MultiConnectionPanel extends JPanel {

    private Port input;

    private ConnectionListModel connectionListModel;
    private JList connectionList;

    public MultiConnectionPanel(Port input) {
        super(new BorderLayout(5, 0));
        setOpaque(false);
        this.input = input;
        connectionListModel = new ConnectionListModel();
        connectionList = new JList(connectionListModel);
        //outputParameterList.setPreferredSize(new Dimension(160, 200));
        PortCellRenderer portCellRenderer = new PortCellRenderer();
        connectionList.setCellRenderer(portCellRenderer);
        connectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
        buttonPanel.setOpaque(false);
        JButton upButton = new JButton(new Icons.ArrowIcon(Icons.ArrowIcon.NORTH));
        upButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveUp();
            }
        });
        JButton downButton = new JButton(new Icons.ArrowIcon(Icons.ArrowIcon.SOUTH));
        downButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveDown();
            }
        });
        JButton removeButton = new JButton(new Icons.MinusIcon());
        removeButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                removeSelected();
            }
        });
        buttonPanel.add(upButton);
        buttonPanel.add(downButton);
        buttonPanel.add(removeButton);
        buttonPanel.setPreferredSize(new Dimension(35, 100));
        add(new JScrollPane(connectionList), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.EAST);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 100);
    }

    public java.util.List<Connection> getConnections() {
        return input.getConnections();
    }

    private void moveDown() {
        moveDelta(1);
    }

    private void moveUp() {
        moveDelta(-1);
    }

    private void moveDelta(int delta) {
        Connection selectedConnection = (Connection) connectionList.getSelectedValue();
        if (selectedConnection == null) return;
        java.util.List<Connection> connections = getConnections();
        int index = connections.indexOf(selectedConnection);
        checkState(index >= 0, "Selected connection %s could not be found.", selectedConnection);
        boolean reordered = input.getParentNode().reorderConnection(selectedConnection, delta);
        if (reordered) {
            reloadList();
            connectionList.setSelectedIndex(index + delta);
        }
    }

    private void removeSelected() {
        Connection selectedConnection = (Connection) connectionList.getSelectedValue();
        if (selectedConnection == null) return;
        input.getParentNode().disconnect(selectedConnection);
        int lastIndex = connectionList.getSelectedIndex();
        reloadList();
        connectionList.setSelectedIndex(Math.max(0, lastIndex - 1));
    }

    private void reloadList() {
        connectionList.setModel(connectionListModel);
        connectionList.repaint();
    }

    private class ConnectionListModel implements ListModel {

        public int getSize() {
            return getConnections().size();
        }

        public Object getElementAt(int index) {
            return getConnections().get(index);
        }

        public void addListDataListener(ListDataListener l) {
        }

        public void removeListDataListener(ListDataListener l) {
        }

    }

    private class PortCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Connection conn = (Connection) value;
            String displayValue = conn.getOutputNode().getName();
            return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
        }
    }

    public static void main(String[] args) {
        NodeLibrary library = new NodeLibrary("test");
        Node mergeShapes = Node.ROOT_NODE.newInstance(library, "mergeshapes", Polygon.class);
        Node poly1 = Node.ROOT_NODE.newInstance(library, "poly1", Polygon.class);
        Node poly2 = Node.ROOT_NODE.newInstance(library, "poly2", Polygon.class);
        Node poly3 = Node.ROOT_NODE.newInstance(library, "poly3", Polygon.class);
        Node poly4 = Node.ROOT_NODE.newInstance(library, "poly4", Polygon.class);
        Port shapesPort = mergeShapes.addPort("shapes", Port.Cardinality.MULTIPLE);
        shapesPort.connect(poly1);
        shapesPort.connect(poly2);
        shapesPort.connect(poly3);
        shapesPort.connect(poly4);

        JDialog d = new JDialog();
        d.setModal(true);
        d.getContentPane().setLayout(new BorderLayout());

        MultiConnectionPanel panel = new MultiConnectionPanel(shapesPort);
        d.getContentPane().add(panel, BorderLayout.CENTER);
        d.setSize(400, 400);
        d.setVisible(true);
    }

}
