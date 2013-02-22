package nodebox.client;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.awt.*;
import javax.swing.JFrame;

public class FullScreenFrame extends JFrame {
    private final NodeBoxDocument document;
    private final Viewer viewer;

    public FullScreenFrame(final NodeBoxDocument document) {
        this.document = document;
        setLayout(new BorderLayout(0, 0));

        viewer = new Viewer();
        document.addZoomListener(viewer);
        add(viewer, BorderLayout.CENTER);

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (gd.isFullScreenSupported()) {
            setUndecorated(true);
            gd.setFullScreenWindow(this);
        } else {
            System.err.println("Full screen not supported");
            setSize(100, 100); // just something to let you see the window
            setVisible(true);
        }
    }

    public Viewer getViewer() {
        return viewer;
    }

    public void setOutputValues(Iterable<?> objects) {
        if (objects == null) {
            viewer.setOutputValues(ImmutableList.of());
        }
        else {
            Iterable<?> nonNulls = Iterables.filter(objects, Predicates.notNull());
            viewer.setOutputValues(ImmutableList.copyOf(ImmutableList.copyOf(nonNulls)));
        }
    }

    public void close() {
        document.closeFullScreenWindow();
    }

    public void toggleAnimation() {
        document.toggleAnimation();
    }

    public void rewindAnimation() {
        document.doRewind();
    }
}
