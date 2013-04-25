package nodebox.client.port;

import nodebox.node.Port;

public class ImageControl extends FileControl {

    public ImageControl(Port port) {
        super(port);
    }

    @Override
    public String acceptedExtensions() {
        return "png,jpg,jpeg,tiff,gif,svg";
    }

    @Override
    public String acceptedDescription() {
        return "Image files";
    }
}
