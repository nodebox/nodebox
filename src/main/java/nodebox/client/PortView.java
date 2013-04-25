package nodebox.client;

import nodebox.client.port.*;
import nodebox.node.Node;
import nodebox.node.Port;
import nodebox.ui.PaneView;
import nodebox.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;

public class PortView extends JComponent implements PaneView, PortControl.OnValueChangeListener {

    private static Logger logger = Logger.getLogger("nodebox.client.PortView");

    private static final Map<Port.Widget, Class> CONTROL_MAP;

    // At this width, the label background lines out with the pane header divider.
    public static final int LABEL_WIDTH = 114;

    static {
        CONTROL_MAP = new HashMap<Port.Widget, Class>();
        CONTROL_MAP.put(Port.Widget.ANGLE, FloatControl.class);
        CONTROL_MAP.put(Port.Widget.COLOR, ColorControl.class);
        CONTROL_MAP.put(Port.Widget.FILE, FileControl.class);
        CONTROL_MAP.put(Port.Widget.FLOAT, FloatControl.class);
        CONTROL_MAP.put(Port.Widget.FONT, FontControl.class);
        CONTROL_MAP.put(Port.Widget.GRADIENT, null);
        CONTROL_MAP.put(Port.Widget.IMAGE, ImageControl.class);
        CONTROL_MAP.put(Port.Widget.INT, IntControl.class);
        CONTROL_MAP.put(Port.Widget.MENU, MenuControl.class);
        CONTROL_MAP.put(Port.Widget.PASSWORD, PasswordControl.class);
        CONTROL_MAP.put(Port.Widget.SEED, IntControl.class);
        CONTROL_MAP.put(Port.Widget.DATA, DataControl.class);
        CONTROL_MAP.put(Port.Widget.STRING, StringControl.class);
        CONTROL_MAP.put(Port.Widget.TEXT, StringControl.class); // TODO TextControl
        CONTROL_MAP.put(Port.Widget.TOGGLE, ToggleControl.class);
        CONTROL_MAP.put(Port.Widget.POINT, PointControl.class);
    }

    private final NodeBoxDocument document;
    private final PortPane pane;
    private JPanel controlPanel;
    private Map<String, PortControl> controlMap = new HashMap<String, PortControl>();

    public PortView(PortPane pane, NodeBoxDocument document) {
        this.pane = pane;
        this.document = document;
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

    public Node getActiveNode() {
        Node activeNode = document.getActiveNode();
        return activeNode != null ? activeNode : document.getActiveNetwork();
    }

    /**
     * Fully rebuild the port view.
     */
    public void updateAll() {
        Node activeNode = getActiveNode();
        if (activeNode == null) {
            pane.setHeaderTitle("Ports");
        } else {
            pane.setHeaderTitle(activeNode.getName());
        }
        rebuildInterface();
        validate();
        repaint();
    }

    /**
     * The port was updated, either its metadata, expression or value.
     * <p/>
     * Rebuild the interface for this port.
     *
     * @param port The updated port.
     */
    public void updatePort(Port port) {
        // TODO More granular rebuild.
        rebuildInterface();
    }

    /**
     * The value for a port was changed.
     * <p/>
     * Display the new value in the port's control UI.
     *
     * @param portName The changed port.
     * @param value    The new port value.
     */
    public void updatePortValue(String portName, Object value) {
        PortControl control = getControlForPort(portName);
        if (control != null && control.isVisible()) {
            control.setValueForControl(value);
        }
    }

    /**
     * Check the enabled state of all Parameters and sync the port rows accordingly.
     */
    public void updateEnabledState() {
        for (Component c : controlPanel.getComponents())
            if (c instanceof PortRow) {
                PortRow row = (PortRow) c;
                if (row.isEnabled() != row.getPort().isEnabled()) {
                    row.setEnabled(row.getPort().isEnabled());
                }
            }
    }

    private void rebuildInterface() {
        controlPanel.removeAll();
        controlMap.clear();
        Node node = getActiveNode();

        if (node == null) return;
        int rowIndex = 0;

        ArrayList<String> portNames = new ArrayList<String>();

        String activeNodePath = document.getActiveNodePath();

        for (Port p : node.getInputs())
            portNames.add(p.getName());

        for (String portName : portNames) {
            Port p = node.getInput(portName);
            // Hide ports with names that start with an underscore.
            if (portName.startsWith("_") || p.getName().startsWith("_")) continue;
            // Hide ports whose values can't be persisted.
            if (p.isCustomType()) continue;
            // Hide ports that accept lists.
            if (p.hasListRange()) continue;
            Class widgetClass = CONTROL_MAP.get(p.getWidget());
            JComponent control;
            if (getDocument().isConnected(portName)) {
                control = new JLabel("<connected>");
                control.setMinimumSize(new Dimension(10, 35));
                control.setFont(Theme.SMALL_FONT);
                control.setForeground(Theme.TEXT_DISABLED_COLOR);
            } else if (widgetClass != null) {
                control = (JComponent) constructControl(widgetClass, activeNodePath, p);
                ((PortControl) control).setValueChangeListener(this);
                ((PortControl) control).setDisplayName(portName);
                controlMap.put(portName, (PortControl) control);
            } else {
                control = new JLabel("  ");
            }

            GridBagConstraints rowConstraints = new GridBagConstraints();
            rowConstraints.gridx = 0;
            rowConstraints.gridy = rowIndex;
            rowConstraints.fill = GridBagConstraints.HORIZONTAL;
            rowConstraints.weightx = 1.0;
            PortRow portRow = new PortRow(getDocument(), portName, control);
            portRow.setEnabled(p.isEnabled());
            controlPanel.add(portRow, rowConstraints);
            rowIndex++;
        }

        if (rowIndex == 0) {
            JLabel noPorts = new JLabel("No ports");
            noPorts.setFont(Theme.SMALL_BOLD_FONT);
            noPorts.setForeground(Theme.TEXT_NORMAL_COLOR);
            controlPanel.add(noPorts);
        }
        JLabel filler = new JLabel();
        GridBagConstraints fillerConstraints = new GridBagConstraints();
        fillerConstraints.gridx = 0;
        fillerConstraints.gridy = rowIndex;
        fillerConstraints.fill = GridBagConstraints.BOTH;
        fillerConstraints.weighty = 1.0;
        fillerConstraints.gridwidth = GridBagConstraints.REMAINDER;
        controlPanel.add(filler, fillerConstraints);
        revalidate();
    }

    @SuppressWarnings("unchecked")
    private PortControl constructControl(Class controlClass, String activeNodePath, Port p) {
        try {
            Constructor constructor = controlClass.getConstructor(String.class, Port.class);
            return (PortControl) constructor.newInstance(activeNodePath, p);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot construct control", e);
            throw new AssertionError("Cannot construct control:" + e);
        }
    }

    public PortControl getControlForPort(String portName) {
        return controlMap.get(portName);
    }

    public void onValueChange(String nodePath, String portName, Object newValue) {
        document.setValue(nodePath, portName, newValue);
    }

    private class ControlPanel extends JPanel {
        private ControlPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (getActiveNode() == null) {
                Rectangle clip = g.getClipBounds();
                g.setColor(new Color(196, 196, 196));
                g.fillRect(clip.x, clip.y, clip.width, clip.height);
            } else {
                int height = getHeight();
                int width = getWidth();
                g.setColor(Theme.PORT_LABEL_BACKGROUND);
                g.fillRect(0, 0, LABEL_WIDTH - 3, height);
                g.setColor(new Color(146, 146, 146));
                g.fillRect(LABEL_WIDTH - 3, 0, 1, height);
                g.setColor(new Color(133, 133, 133));
                g.fillRect(LABEL_WIDTH - 2, 0, 1, height);
                g.setColor(new Color(112, 112, 112));
                g.fillRect(LABEL_WIDTH - 1, 0, 1, height);
                g.setColor(Theme.PORT_VALUE_BACKGROUND);
                g.fillRect(LABEL_WIDTH, 0, width - LABEL_WIDTH, height);
            }
        }
    }
}
