package nodebox.client;

import nodebox.client.parameter.*;
import nodebox.node.Node;
import nodebox.node.Parameter;
import nodebox.node.Port;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParameterView extends JComponent implements PaneView, ParameterControl.OnValueChangeListener {

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

    // TODO Remove the reference to the document.
    private NodeBoxDocument document;
    private Pane pane;
    private Node activeNode;
    private JPanel controlPanel;
    private Map<Parameter, ParameterControl> controlMap = new HashMap<Parameter, ParameterControl>();
    private MultiConnectionPanel multiConnectionPanel;

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
    }

    public NodeBoxDocument getDocument() {
        return document;
    }

    public void setDocument(NodeBoxDocument document) {
        this.document = document;
    }

    public Node getActiveNode() {
        return activeNode;
    }

    public void setActiveNode(Node node) {
        this.activeNode = node;
        rebuildInterface();
        validate();
        repaint();
    }

    /**
     * Fully rebuild the parameter view.
     */
    public void updateAll() {
        rebuildInterface();
    }

    /**
     * The parameter was updated, either its metadata, expression or value.
     * <p/>
     * Rebuild the interface for this parameter.
     *
     * @param parameter The updated parameter.
     */
    public void updateParameter(Parameter parameter) {
        // TODO More granular rebuild.
        rebuildInterface();
    }

    /**
     * The value for a parameter was changed.
     * <p/>
     * Display the new value in the parameter's control UI.
     *
     * @param parameter The changed parameter.
     * @param value     The new parameter value.
     */
    public void updateParameterValue(Parameter parameter, Object value) {
        // Nodes that have expressions set don't display the actual value but the expression.
        // Since the expression doesn't change, we can return immediately.
        if (parameter.hasExpression()) return;
        ParameterControl control = getControlForParameter(parameter);
        if (control != null && control.isVisible()) {
            control.setValueForControl(value);
        }
    }

    /**
     * Check the enabled state of all Parameters and sync the parameter rows accordingly.
     */
    public void updateEnabledState() {
        for (Component c : controlPanel.getComponents())
        if (c instanceof ParameterRow) {
            ParameterRow row = (ParameterRow) c;
            if (row.isEnabled() != row.getParameter().isEnabled()) {
                row.setEnabled(row.getParameter().isEnabled());
            }
        }
    }

    // Update the multi-connection panel in the parameter view.
    public void updateConnectionPanel() {
        if (multiConnectionPanel != null) {
            multiConnectionPanel.update();
        }
    }

    private void rebuildInterface() {
        controlPanel.removeAll();
        controlMap.clear();
        if (activeNode == null) return;
        int rowindex = 0;
        for (Port p : activeNode.getPorts()) {
            if (p.getCardinality() != Port.Cardinality.MULTIPLE) continue;
            multiConnectionPanel = new MultiConnectionPanel(getDocument(), p);
            PortRow portRow = new PortRow(p, multiConnectionPanel);
            GridBagConstraints rowConstraints = new GridBagConstraints();
            rowConstraints.gridx = 0;
            rowConstraints.gridy = rowindex;
            rowConstraints.fill = GridBagConstraints.HORIZONTAL;
            rowConstraints.weightx = 1.0;
            controlPanel.add(portRow, rowConstraints);
            rowindex++;
        }

        for (Parameter p : activeNode.getParameters()) {
            // Parameters starting with underscores are hidden.
            boolean nodeDescriptionShown = p.getName().equals("_description") &&
                    !p.prototypeEquals(p.getPrototype());
            if (p.getName().startsWith("_") && !nodeDescriptionShown) continue;
            Class widgetClass = CONTROL_MAP.get(p.getWidget());
            JComponent control;
            if (widgetClass != null) {
                control = (JComponent) constructControl(widgetClass, p);
                ((ParameterControl) control).setValueChangeListener(this);
                controlMap.put(p, (ParameterControl) control);
            } else {
                control = new JLabel("  ");
            }

            GridBagConstraints rowConstraints = new GridBagConstraints();
            rowConstraints.gridx = 0;
            rowConstraints.gridy = rowindex;
            rowConstraints.fill = GridBagConstraints.HORIZONTAL;
            rowConstraints.weightx = 1.0;
            if (!nodeDescriptionShown) {
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
            Constructor constructor = controlClass.getConstructor(Parameter.class);
            return (ParameterControl) constructor.newInstance(p);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot construct control", e);
            throw new AssertionError("Cannot construct control:" + e);
        }
    }

    public ParameterControl getControlForParameter(Parameter p) {
        return controlMap.get(p);
    }

    public void onValueChange(ParameterControl control, Object newValue) {
        document.setParameterValue(control.getParameter(), newValue);
    }

    private class ControlPanel extends JPanel {
        private ControlPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (activeNode == null) {
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
