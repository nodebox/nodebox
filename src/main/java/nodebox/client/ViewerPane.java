package nodebox.client;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import nodebox.handle.Handle;
import nodebox.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ViewerPane extends Pane {

    private final static String VIEWER_CARD = "viewer";
    private final static String DATA_SHEET_CARD = "data-sheet";

    private final NodeBoxDocument document;
    private final PaneHeader paneHeader;
    private final Viewer viewer;
    private final DataSheet dataSheet;
    private final NButton handlesCheck, pointsCheck, pointNumbersCheck, originCheck, boundsCheck, renderRootCheck;
    private final JPanel contentPanel;
    private OutputView currentView;
    private Iterable<?> outputValues;
    private final PaneTab viewerToggle;
    private final PaneTab dataSheetToggle;

    public ViewerPane(final NodeBoxDocument document) {
        this.document = document;
        setLayout(new BorderLayout(0, 0));

        contentPanel = new JPanel(new CardLayout());
        viewer = new Viewer();
        document.addZoomListener(viewer);
        currentView = viewer;
        dataSheet = new DataSheet();
        contentPanel.add(viewer, VIEWER_CARD);
        contentPanel.add(dataSheet, DATA_SHEET_CARD);
        add(contentPanel, BorderLayout.CENTER);

        paneHeader = PaneHeader.withoutTitle();

        viewerToggle = new PaneTab(new SwitchCardAction("Viewer", VIEWER_CARD, viewer));
        viewerToggle.setFont(Theme.SMALL_FONT);
        viewerToggle.setSelected(true);
        dataSheetToggle = new PaneTab(new SwitchCardAction("Data", DATA_SHEET_CARD, dataSheet));
        paneHeader.add(viewerToggle);
        paneHeader.add(dataSheetToggle);
        paneHeader.add(new Divider());

        handlesCheck = new NButton(NButton.Mode.CHECK, "Handles");
        handlesCheck.setChecked(true);
        handlesCheck.setActionMethod(this, "toggleHandles");
        handlesCheck.setToolTipText("Show/Hide handles");
        pointsCheck = new NButton(NButton.Mode.CHECK, "Points");
        pointsCheck.setActionMethod(this, "togglePoints");
        pointNumbersCheck = new NButton(NButton.Mode.CHECK, "Point Numbers");
        pointNumbersCheck.setActionMethod(this, "togglePointNumbers");
        originCheck = new NButton(NButton.Mode.CHECK, "Origin");
        originCheck.setActionMethod(this, "toggleOrigin");
        boundsCheck = new NButton(NButton.Mode.CHECK, "Bounds");
        boundsCheck.setActionMethod(this, "toggleBounds");
        renderRootCheck = new NButton(NButton.Mode.CHECK, "Render Root");
        renderRootCheck.setActionMethod(this, "toggleRenderRoot");
        renderRootCheck.setChecked(true);
        paneHeader.add(handlesCheck);
        paneHeader.add(pointsCheck);
        paneHeader.add(pointNumbersCheck);
        paneHeader.add(originCheck);
        paneHeader.add(boundsCheck);
        paneHeader.add(renderRootCheck);
        add(paneHeader, BorderLayout.NORTH);
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

    public void setOutputValues(Iterable<?> objects) {
        if (objects == null) {
            this.outputValues = ImmutableList.of();
            currentView.setOutputValues(ImmutableList.of());
        } else {
            Iterable<?> nonNulls = Iterables.filter(objects, Predicates.notNull());
            this.outputValues = ImmutableList.copyOf(nonNulls);
            currentView.setOutputValues(ImmutableList.copyOf(this.outputValues));
        }
    }

    public void toggleOrigin() {
        viewer.setShowOrigin(originCheck.isChecked());
    }

    public void toggleBounds() {
        viewer.setShowBounds(boundsCheck.isChecked());
    }

    public void toggleRenderRoot() {
        document.requestRender();
    }

    public boolean shouldAlwaysRenderRoot() {
        return renderRootCheck.isChecked();
    }

    public Pane duplicate() {
        return new ViewerPane(document);
    }

    public PaneHeader getPaneHeader() {
        return paneHeader;
    }

    public PaneView getPaneView() {
        return currentView;
    }

    public DataSheet getDataSheet() {
        return dataSheet;
    }

    public Viewer getViewer() {
        return viewer;
    }

    public Handle getHandle() {
        return viewer.getHandle();
    }

    public void setHandle(Handle handle) {
        viewer.setHandle(handle);
    }

    public void updateHandle() {
        viewer.updateHandle();
    }

    private class SwitchCardAction extends AbstractAction {

        private final String viewName;
        private final OutputView view;

        private SwitchCardAction(String s, String viewName, OutputView view) {
            super(s);
            this.viewName = viewName;
            this.view = view;
        }

        public void actionPerformed(ActionEvent e) {
            CardLayout layout = (CardLayout) contentPanel.getLayout();
            layout.show(contentPanel, viewName);
            viewerToggle.setSelected(view == viewer);
            dataSheetToggle.setSelected(view == dataSheet);
            currentView = view;
            currentView.setOutputValues(ImmutableList.copyOf(outputValues));
        }

    }

    private final class PaneTab extends JToggleButton {

        private PaneTab(Action action) {
            super(action);
            Dimension d = new Dimension(50, 21);
            setMinimumSize(d);
            setMaximumSize(d);
            setPreferredSize(d);
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);
            if (selected) {
                setBorder(Theme.INNER_SHADOW_BORDER);
            } else {
                setBorder(Theme.EMPTY_BORDER);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g.setFont(Theme.SMALL_BOLD_FONT);

            if (isSelected()) {
                g.setColor(Theme.SELECTED_TAB_BACKGROUND_COLOR);
                g2.fill(g.getClip());
                g.setColor(Theme.TEXT_NORMAL_COLOR);
            } else {
                g.setColor(Theme.TAB_BACKGROUND_COLOR);
                g2.fill(g.getClip());
                g.setColor(Theme.TEXT_DISABLED_COLOR);
            }

            SwingUtils.drawShadowText((Graphics2D) g, getText(), 5, 14);
        }
    }

}
