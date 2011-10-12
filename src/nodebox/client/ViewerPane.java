package nodebox.client;

import java.awt.*;

public class ViewerPane extends Pane {

    // TODO This is only kept here for duplicating panes.
    private final NodeBoxDocument document;
    private PaneHeader paneHeader;
    private Viewer viewer;
    private NButton handlesCheck, pointsCheck, pointNumbersCheck, originCheck;

    public ViewerPane(final NodeBoxDocument document) {
        this.document = document;
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        handlesCheck = new NButton(NButton.Mode.CHECK, "Handles");
        handlesCheck.setChecked(true);
        handlesCheck.setActionMethod(this, "toggleHandles");
        pointsCheck = new NButton(NButton.Mode.CHECK, "Points");
        pointsCheck.setActionMethod(this, "togglePoints");
        pointNumbersCheck = new NButton(NButton.Mode.CHECK, "Point Numbers");
        pointNumbersCheck.setActionMethod(this, "togglePointNumbers");
        originCheck = new NButton(NButton.Mode.CHECK, "Origin");
        originCheck.setActionMethod(this, "toggleOrigin");
        paneHeader.add(handlesCheck);
        paneHeader.add(pointsCheck);
        paneHeader.add(pointNumbersCheck);
        paneHeader.add(originCheck);
        viewer = new Viewer(document);
        add(paneHeader, BorderLayout.NORTH);
        add(viewer, BorderLayout.CENTER);
    }

    public Viewer getViewer() {
        return viewer;
    }

    public void toggleHandles() {
        viewer.setShowHandle(handlesCheck.isChecked());
    }

    public void togglePoints() {
        viewer.setShowPoints(pointsCheck.isChecked());
    }

    public void togglePointNumbers() {
        viewer.setShowPointNumbers(pointNumbersCheck.isChecked());
    }

    public void toggleOrigin() {
        viewer.setShowOrigin(originCheck.isChecked());
    }

    public Pane duplicate() {
        return new ViewerPane(document);
    }

    public String getPaneName() {
        return "Viewer";
    }

    public PaneHeader getPaneHeader() {
        return paneHeader;
    }

    public PaneView getPaneView() {
        return viewer;
    }
}
