package nodebox.client.parameter;

import nodebox.node.Parameter;

public class ImageControl extends FileControl {

    public ImageControl(Parameter parameter) {
        super(parameter);
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
