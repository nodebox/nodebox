package nodebox.client;

import nodebox.Icons;
import nodebox.node.Connection;
import nodebox.node.Node;
import nodebox.node.Port;
import nodebox.node.NodeLibrary;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MultiConnectionPanel extends JPanel {

    private Port input;

    private PortListModel portListModel;
    private JList outputList;

    public MultiConnectionPanel(Port input) {
        super(new BorderLayout(5, 0));
        setOpaque(false);
        this.input = input;
        portListModel = new PortListModel();
        outputList = new JList(portListModel);
        //outputParameterList.setPreferredSize(new Dimension(160, 200));
        PortCellRenderer portCellRenderer = new PortCellRenderer();
        outputList.setCellRenderer(portCellRenderer);
        outputList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        add(new JScrollPane(outputList), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.EAST);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 100);
    }

    public Connection getConnection() {
        return input.getConnection();
    }

    private void moveDown() {
        Connection c = getConnection();
        if (c == null) return;
        Port selectedPort = (Port) outputList.getSelectedValue();
        if (selectedPort == null) return;
        java.util.List<Port> ports = c.getOutputs();
        int index = ports.indexOf(selectedPort);
        assert (index >= 0);
        if (index >= ports.size() - 1) return;
        c.reorderOutput(selectedPort, 1);
        reloadList();
        outputList.setSelectedIndex(index + 1);
        input.getNode().markDirty();
    }

    private void moveUp() {
        Connection c = getConnection();
        if (c == null) return;
        Port selectedPort = (Port) outputList.getSelectedValue();
        if (selectedPort == null) return;
        java.util.List<Port> ports = c.getOutputs();
        int index = ports.indexOf(selectedPort);
        assert (index >= 0);
        if (index == 0) return;
        c.reorderOutput(selectedPort, -1);
        reloadList();
        outputList.setSelectedIndex(index - 1);
        input.getNode().markDirty();
    }

    private void removeSelected() {
        Port output = (Port) outputList.getSelectedValue();
        if (output == null) return;
        Node node = input.getNode();
        node.disconnect(input, output.getNode());
        reloadList();
    }

    private void reloadList() {
        outputList.setModel(portListModel);
        outputList.repaint();
    }

    private class PortListModel implements ListModel {
        public int getSize() {
            Connection c = getConnection();
            if (c == null) return 0;
            return c.getOutputs().size();
        }

        public Object getElementAt(int index) {
            Connection c = getConnection();
            if (c == null) return 0;
            return c.getOutputs().get(index);
        }

        public void addListDataListener(ListDataListener l) {
        }

        public void removeListDataListener(ListDataListener l) {
        }
    }

    private class PortCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Port port = (Port) value;
            String displayValue = port.getNode().getName();
            return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
        }
    }
    public static void main(String[] args) {
        NodeLibrary library = new NodeLibrary("test");
        Node mergeShapes = Node.ROOT_NODE.newInstance(library, "mergeshapes", Polygon.class);
        Node poly1 = Node.ROOT_NODE.newInstance(library, "poly1", Polygon.class);
        Node poly2 = Node.ROOT_NODE.newInstance(library, "poly2", Polygon.class);
        Node poly3 = Node.ROOT_NODE.newInstance(library, "poly3", Polygon.class);
        Port shapesPort = mergeShapes.addPort("shapes", Port.Cardinality.MULTIPLE);
        shapesPort.connect(poly1);
        shapesPort.connect(poly2);
        shapesPort.connect(poly3);

        JDialog d = new JDialog();
        d.setModal(true);
        d.getContentPane().setLayout(new BorderLayout());

        MultiConnectionPanel panel = new MultiConnectionPanel(shapesPort);
        d.getContentPane().add(panel, BorderLayout.CENTER);
        d.setSize(400, 400);
        d.setVisible(true);
    }

}
