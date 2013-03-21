package nodebox.client.visualizer;

import com.google.common.collect.ImmutableList;

public class VisualizerFactory {

    private static final Visualizer DEFAULT_VISUALIZER = LastResortVisualizer.INSTANCE;
    private static final ImmutableList<Visualizer> visualizers;

    static {
        visualizers = ImmutableList.of(CanvasVisualizer.INSTANCE, GrobVisualizer.INSTANCE, PointVisualizer.INSTANCE, ColorVisualizer.INSTANCE);
    }

    public static Visualizer getDefaultVisualizer() {
        return DEFAULT_VISUALIZER;
    }

    public static Visualizer getVisualizer(Iterable<?> objects, Class listClass) {
        for (Visualizer visualizer : visualizers) {
            if (visualizer.accepts(objects, listClass))
                return visualizer;
        }
        return DEFAULT_VISUALIZER;
    }

}
