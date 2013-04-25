package nodebox.client.port;

import nodebox.node.Port;

public class ImageControl extends FileControl {

    public ImageControl(String nodePath, Port port) {
        super(nodePath, port);
    }

    @Override
    public String acceptedExtensions() {
        return "png,jpg,jpeg,tiff,gif";
    }

    @Override
    public String acceptedDescription() {
        return "Image files";
    }
}
