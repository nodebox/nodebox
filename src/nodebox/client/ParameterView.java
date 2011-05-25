package nodebox.client;

import nodebox.client.parameter.*;
import nodebox.node.*;
import nodebox.node.event.ConnectionAddedEvent;
import nodebox.node.event.NodeAttributeChangedEvent;
import nodebox.node.event.ValueChangedEvent;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParameterView extends JComponent implements PaneView, NodeEventListener {

    private static Logger logger = Logger.getLogger("nodebox.client.ParameterView");

    private static final Map<Parameter.Widget, Class> CONTROL_MAP;

    // At this width, the label background lines out with the pane header divider.
    public static final int LABEL_WIDTH = 114;

    static {
        CONTROL_MAP = new HashMap<Parameter.Widget, Class>();
        CONTROL_MAP.put(Parameter.Widget.ANGLE, FloatControl.class);
        CONTROL_MAP.put(Parameter.Widget.COLOR, ColorControl.class);
        CONTROL_MAP.put(Parameter.Widget.FILE, FileControl.class);
        CONTROL_MAP.put(Parameter.Widget.FLOAT, FloatControl.class);
        CONTROL_MAP.put(Parameter.Widget.FONT, FontControl.class);
        CONTROL_MAP.put(Parameter.Widget.GRADIENT, null);
        CONTROL_MAP.put(Parameter.Widget.IMAGE, ImageControl.class);
        CONTROL_MAP.put(Parameter.Widget.INT, IntControl.class);
        CONTROL_MAP.put(Parameter.Widget.MENU, MenuControl.class);
        CONTROL_MAP.put(Parameter.Widget.SEED, IntControl.class);
        CONTROL_MAP.put(Parameter.Widget.STRING, StringControl.class);
        CONTROL_MAP.put(Parameter.Widget.TEXT, TextControl.class);
        CONTROL_MAP.put(Parameter.Widget.TOGGLE, ToggleControl.class);
        CONTROL_MAP.put(Parameter.Widget.NODEREF, NoderefControl.class);
        CONTROL_MAP.put(Parameter.Widget.STAMP_EXPRESSION, StampExpressionControl.class);
    }

    private Pane pane;
    private Node node;
    private JPanel controlPanel;
    private Map<Parameter, ParameterControl> controlMap = new HashMap<Parameter, ParameterControl>();

    public ParameterView(Pane pane) {
        this.pane = pane;
        setLayout(new BorderLayout());
        controlPanel = new ControlPanel(new GridBagLayout());
        // controlPanel = new JPanel(new GridBagLayout());
        //controlPanel.setOpaque(false);
        //controlPanel.setBackground(Theme.getInstance().getParameterViewBackgroundColor());
        JScrollPane scrollPane = new JScrollPane(controlPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
        getDocument().getNodeLibrary().addListener(this);
    }

    public NodeBoxDocument getDocument() {
        return pane.getDocument();
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        // If this is the first time the node is set, register to the library.
        // This only needs to be done once, and this view is never re-used in another library.
        if (this.node == null) {
            node.getLibrary().addListener(this);
        }
        if (node == null) return;
        this.node = node;
        rebuildInterface();
        validate();
        repaint();
    }

    private void rebuildInterface() {
        controlPanel.removeAll();
        controlMap.clear();
        if (node == null) return;
        int rowindex = 0;
        for (Port p : node.getPorts()) {
            if (p.getCardinality() != Port.Cardinality.MULTIPLE) continue;
            MultiConnectionPanel panel = new MultiConnectionPanel(p);
            PortRow portRow = new PortRow(p, panel);
            GridBagConstraints rowConstraints = new GridBagConstraints();
            rowConstraints.gridx = 0;
            rowConstraints.gridy = rowindex;
            rowConstraints.fill = GridBagConstraints.HORIZONTAL;
            rowConstraints.weightx = 1.0;
            controlPanel.add(portRow, rowConstraints);
            rowindex++;
        }

        for (Parameter p : node.getParameters()) {
            // Parameters starting with underscores are hidden.
            boolean nodeDescriptionShown = p.getName().equals("_description") &&
                                                              !p.prototypeEquals(p.getPrototype());
            if (p.getName().startsWith("_") && !nodeDescriptionShown) continue;
            Class widgetClass = CONTROL_MAP.get(p.getWidget());
            JComponent control;
            if (widgetClass != null) {
                control = (JComponent) constructControl(widgetClass, p);
                controlMap.put(p, (ParameterControl) control);
            } else {
                control = new JLabel("  ");
            }

            GridBagConstraints rowConstraints = new GridBagConstraints();
            rowConstraints.gridx = 0;
            rowConstraints.gridy = rowindex;
            rowConstraints.fill = GridBagConstraints.HORIZONTAL;
            rowConstraints.weightx = 1.0;
            if (! nodeDescriptionShown) {
                ParameterRow parameterRow = new ParameterRow(getDocument(), p, control);
                parameterRow.setEnabled(p.isEnabled());
                controlPanel.add(parameterRow, rowConstraints);
            } else {
                ParameterNotesRow row = new ParameterNotesRow(p.asString());
                controlPanel.add(row, rowConstraints);
            }
            rowindex++;
        }

        if (rowindex == 0) {
            JLabel noParameters = new JLabel("No parameters");
            noParameters.setFont(Theme.SMALL_BOLD_FONT);
            noParameters.setForeground(Theme.TEXT_NORMAL_COLOR);
            controlPanel.add(noParameters);
        }
        JLabel filler = new JLabel();
        GridBagConstraints fillerConstraints = new GridBagConstraints();
        fillerConstraints.gridx = 0;
        fillerConstraints.gridy = rowindex;
        fillerConstraints.fill = GridBagConstraints.BOTH;
        fillerConstraints.weighty = 1.0;
        fillerConstraints.gridwidth = GridBagConstraints.REMAINDER;
        controlPanel.add(filler, fillerConstraints);
        revalidate();
    }

    private ParameterControl constructControl(Class controlClass, Parameter p) {
        try {
            Constructor constructor = controlClass.getConstructor(NodeBoxDocument.class, Parameter.class);
            return (ParameterControl) constructor.newInstance(getDocument(), p);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot construct control", e);
            throw new AssertionError("Cannot construct control:" + e);
        }
    }

    public ParameterControl getControlForParameter(Parameter p) {
        return controlMap.get(p);
    }

    public void receive(NodeEvent event) {
        if (event instanceof ValueChangedEvent) {
            if (event.getSource() != node) return;
            ValueChangedEvent e = (ValueChangedEvent) event;
            Parameter p = e.getParameter();
            // Nodes that have expressions set don't display the actual value but the expression.
            // Since the expression doesn't change, we can return immediately.
            if (p.hasExpression()) return;
            ParameterControl control = getControlForParameter(p);
            if (control != null && control.isVisible()) {
                control.setValueForControl(e.getParameter().getValue());
            }
        } else if (event instanceof ConnectionAddedEvent) {
            // We need to know when connections are changed because we display multi-port
            // connections in the parameter view to allow users to order and remove them.
            if (((ConnectionAddedEvent) event).getConnection().getInputNode() == node)
                rebuildInterface();
        } else if (event instanceof NodeAttributeChangedEvent) {
            // Rebuild the interface when one of the node attributes is changed.
            // We don't care about the position, since we don't actually display it.
            if (((NodeAttributeChangedEvent) event).getAttribute() == Node.Attribute.POSITION) return;
            if (event.getSource() != node) return;
            rebuildInterface();
        }
    }

    private class ControlPanel extends JPanel {
        private ControlPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (node == null) {
                Rectangle clip = g.getClipBounds();
                g.setColor(new Color(196, 196, 196));
                g.fillRect(clip.x, clip.y, clip.width, clip.height);
            } else {
                int height = getHeight();
                int width = getWidth();
                g.setColor(Theme.PARAMETER_LABEL_BACKGROUND);
                g.fillRect(0, 0, LABEL_WIDTH - 3, height);
                g.setColor(new Color(146, 146, 146));
                g.fillRect(LABEL_WIDTH - 3, 0, 1, height);
                g.setColor(new Color(133, 133, 133));
                g.fillRect(LABEL_WIDTH - 2, 0, 1, height);
                g.setColor(new Color(112, 112, 112));
                g.fillRect(LABEL_WIDTH - 1, 0, 1, height);
                g.setColor(Theme.PARAMETER_VALUE_BACKGROUND);
                g.fillRect(LABEL_WIDTH, 0, width - LABEL_WIDTH, height);
            }
        }
    }
}
