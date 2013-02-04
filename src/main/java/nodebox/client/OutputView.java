package nodebox.client;

import nodebox.ui.PaneView;

import java.util.List;

/**
 * A class that can show the output of the network.
 */
public interface OutputView extends PaneView {

    public void setOutputValues(List<?> objects);

}
