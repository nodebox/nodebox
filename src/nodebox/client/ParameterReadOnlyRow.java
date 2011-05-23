package nodebox.client;

import nodebox.node.Parameter;

import javax.swing.*;
import java.awt.*;

public class ParameterReadOnlyRow extends JComponent {

    private NodeBoxDocument document;
    private Parameter parameter;
    private JLabel label;
    private JLabel descriptionLabel;

    private static final int TOP_PADDING = 2;
    private static final int BOTTOM_PADDING = 2;

    public ParameterReadOnlyRow(NodeBoxDocument document, Parameter parameter) {
        this.document = document;
        this.parameter = parameter;

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        label = new ShadowLabel(parameter.getLabel());
        label.setToolTipText(parameter.getName());
        label.setBorder(null);
        label.setPreferredSize(new Dimension(ParameterView.LABEL_WIDTH, 16));

        descriptionLabel = new JLabel(parameter.asString());
        descriptionLabel.setFont(Theme.SMALL_BOLD_FONT);
        descriptionLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(TOP_PADDING, 0, BOTTOM_PADDING, 0));

        add(this.label);
        add(Box.createHorizontalStrut(10));
        add(this.descriptionLabel);
        add(Box.createHorizontalGlue());
        // Compensate for the popup button.
        add(Box.createHorizontalStrut(30));
        setBorder(Theme.PARAMETER_ROW_BORDER);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, descriptionLabel.getPreferredSize().height + TOP_PADDING + BOTTOM_PADDING);
    }
}
