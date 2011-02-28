package nodebox.client.parameter;

import nodebox.client.NodeBoxDocument;
import nodebox.node.Parameter;

public class ImageControl extends FileControl {

    public ImageControl(NodeBoxDocument document, Parameter parameter) {
        super(document, parameter);
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
