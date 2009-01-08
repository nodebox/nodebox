package net.nodebox.node.image;

import net.nodebox.graphics.Image;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class ImageType extends ImageNodeType {

    public ImageType(NodeTypeLibrary library) {
        super(library, "image");
        ParameterType pFileName = addParameterType("fileName", ParameterType.Type.IMAGE);
        ParameterType pX = addParameterType("x", ParameterType.Type.FLOAT);
        ParameterType pY = addParameterType("y", ParameterType.Type.FLOAT);
        ParameterType pWidth = addParameterType("width", ParameterType.Type.FLOAT);
        pWidth.setDefaultValue(0.0);
        ParameterType pHeight = addParameterType("height", ParameterType.Type.FLOAT);
        pHeight.setDefaultValue(0.0);
        ParameterType pAlpha = addParameterType("alpha", ParameterType.Type.FLOAT);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        Image img = new Image(node.asString("fileName"));
        img.setX(node.asFloat("x"));
        img.setY(node.asFloat("y"));
        img.setWidth(node.asFloat("width"));
        img.setHeight(node.asFloat("height"));
        img.setAlpha(node.asFloat("alpha"));
        node.setOutputValue(img);
        return true;
    }
}
