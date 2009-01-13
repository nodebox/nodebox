package net.nodebox.client;

import net.nodebox.Icons;
import net.nodebox.node.Connection;
import net.nodebox.node.Network;
import net.nodebox.node.NodeTypeLibraryManager;
import net.nodebox.node.Parameter;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class MultiConnectionPanel extends JPanel {

    private Parameter parameter;

    private ParameterListModel parameterListModel;
    private JList outputParameterList;

    public MultiConnectionPanel(Parameter parameter) {
        super(new BorderLayout(5, 0));
        setBackground(Theme.getInstance().getParameterViewBackgroundColor());
        this.parameter = parameter;
        parameterListModel = new ParameterListModel();
        outputParameterList = new JList(parameterListModel);
        //outputParameterList.setPreferredSize(new Dimension(160, 200));
        ParameterCellRenderer parameterCellRenderer = new ParameterCellRenderer();
        outputParameterList.setCellRenderer(parameterCellRenderer);
        outputParameterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
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
        add(new JScrollPane(outputParameterList), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.EAST);
    }

    public static void main(String[] args) {
        Network net = Network.load(new NodeTypeLibraryManager(), new File("/Users/fdb/Desktop/mergetest2.ndbx"));
        Parameter p = net.getNode("merge1").getParameter("shapes");
        JDialog d = new JDialog();
        d.setModal(true);
        d.getContentPane().setLayout(new BorderLayout());

        MultiConnectionPanel panel = new MultiConnectionPanel(p);
        d.getContentPane().add(panel, BorderLayout.CENTER);
        d.setSize(400, 400);
        d.setVisible(true);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 100);
    }

    public Connection getConnection() {
        return parameter.getExplicitConnection();
    }

    private void moveDown() {
        Connection c = getConnection();
        if (c == null) return;
        Parameter selectedParameter = (Parameter) outputParameterList.getSelectedValue();
        if (selectedParameter == null) return;
        java.util.List<Parameter> parameters = c.getOutputParameters();
        int index = parameters.indexOf(selectedParameter);
        assert (index >= 0);
        if (index >= parameters.size() - 1) return;
        parameters.remove(selectedParameter);
        parameters.add(index + 1, selectedParameter);
        reloadList();
        outputParameterList.setSelectedIndex(index + 1);
        parameter.getNode().markDirty();
    }

    private void moveUp() {
        Connection c = getConnection();
        if (c == null) return;
        Parameter selectedParameter = (Parameter) outputParameterList.getSelectedValue();
        if (selectedParameter == null) return;
        java.util.List<Parameter> parameters = c.getOutputParameters();
        int index = parameters.indexOf(selectedParameter);
        assert (index >= 0);
        if (index == 0) return;
        parameters.remove(selectedParameter);
        parameters.add(index - 1, selectedParameter);
        reloadList();
        outputParameterList.setSelectedIndex(index - 1);
        parameter.getNode().markDirty();
    }


    private void removeSelected() {
        Parameter outputParameter = (Parameter) outputParameterList.getSelectedValue();
        if (outputParameter == null) return;
        Network network = parameter.getNetwork();
        network.disconnect(outputParameter, parameter);
        reloadList();
        parameter.getNode().markDirty();
    }

    private void reloadList() {
        outputParameterList.setModel(parameterListModel);
        outputParameterList.repaint();
    }


    private class ParameterListModel implements ListModel {
        public int getSize() {
            Connection c = getConnection();
            if (c == null) return 0;
            return c.getOutputParameters().size();
        }

        public Object getElementAt(int index) {
            Connection c = getConnection();
            if (c == null) return 0;
            return c.getOutputParameters().get(index);
        }

        public void addListDataListener(ListDataListener l) {
        }

        public void removeListDataListener(ListDataListener l) {
        }
    }

    private class ParameterCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Parameter parameter = (Parameter) value;
            String displayValue = parameter.getNode().getName();
            return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
        }
    }

}
