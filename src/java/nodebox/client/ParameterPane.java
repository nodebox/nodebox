package nodebox.client;

import java.awt.*;

public class ParameterPane extends Pane {

    private PaneHeader paneHeader;
    private final ParameterView parameterView;
    private EditMetadataListener editMetadataListener;

    public ParameterPane() {
        setLayout(new BorderLayout());
        paneHeader = new PaneHeader(this);
        NButton metadataButton = new NButton("Metadata", "res/parameter-metadata.png");
        metadataButton.setActionMethod(this, "editMetadata");
        paneHeader.add(metadataButton);
        parameterView = new ParameterView(this);
        add(paneHeader, BorderLayout.NORTH);
        add(parameterView, BorderLayout.CENTER);
    }

    public ParameterView getParameterView() {
        return parameterView;
    }

    public Pane duplicate() {
        return new ParameterPane();
    }

    public String getPaneName() {
        return "Parameters";
    }

    public PaneHeader getPaneHeader() {
        return paneHeader;
    }

    public PaneView getPaneView() {
        return parameterView;
    }

    public void editMetadata() {
        editMetadataListener.onEditMetadata();
    }

    public EditMetadataListener getEditMetadataListener() {
        return editMetadataListener;
    }

    public void setEditMetadataListener(EditMetadataListener editMetadataListener) {
        this.editMetadataListener = editMetadataListener;
    }

    public static interface EditMetadataListener {

        public void onEditMetadata();


    }

}
