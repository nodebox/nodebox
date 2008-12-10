package net.nodebox.client;

import javax.swing.*;
import java.awt.*;

public class PaneSplitter extends JSplitPane {

    public PaneSplitter(int orientation, Component newLeftComponent, Component newRightComponent) {
        super(orientation, newLeftComponent, newRightComponent);
        setContinuousLayout(true);
        setResizeWeight(0.5);
        setDividerSize(2);
        setBorder(BorderFactory.createEmptyBorder());
    }
}
