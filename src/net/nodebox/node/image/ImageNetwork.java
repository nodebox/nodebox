package net.nodebox.node.image;

import net.nodebox.node.Network;
import net.nodebox.node.Parameter;

public class ImageNetwork extends Network {

    public ImageNetwork() {
        super(Parameter.Type.GROB_IMAGE);
    }

    public ImageNetwork(String name) {
        super(Parameter.Type.GROB_IMAGE, name);
    }
}
