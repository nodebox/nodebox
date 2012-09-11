package nodebox.client;

import java.awt.*;
import java.util.ArrayList;

public class ParameterLayout implements LayoutManager {

    private ArrayList<Component> components;

    public ParameterLayout() {
        components = new ArrayList<Component>();
    }

    public void addLayoutComponent(String name, Component comp) {
        components.add(comp);
    }

    public void removeLayoutComponent(Component comp) {
        components.remove(comp);
    }

    public Dimension preferredLayoutSize(Container parent) {
        return new Dimension(100, 100);
    }

    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(100, 100);
    }

    public void layoutContainer(Container parent) {
    }
}
