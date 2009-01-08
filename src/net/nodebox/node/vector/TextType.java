package net.nodebox.node.vector;

import net.nodebox.graphics.Group;
import net.nodebox.graphics.Text;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class TextType extends VectorNodeType {

    public TextType(NodeTypeLibrary library) {
        super(library, "text");
        setDescription("Creates a text object.");
        ParameterType pText = addParameterType("text", ParameterType.Type.TEXT);
        pText.setDefaultValue("hello");
        ParameterType pX = addParameterType("x", ParameterType.Type.FLOAT);
        ParameterType pY = addParameterType("y", ParameterType.Type.FLOAT);
        pY.setDefaultValue(24.0);
        ParameterType pWidth = addParameterType("width", ParameterType.Type.FLOAT);
        pWidth.setDefaultValue(1000.0);
        ParameterType pHeight = addParameterType("height", ParameterType.Type.FLOAT);
        pHeight.setDefaultValue(1000.0);
        ParameterType pFontName = addParameterType("fontName", ParameterType.Type.FONT);
        pFontName.setDefaultValue("Arial");
        ParameterType pFontSize = addParameterType("fontSize", ParameterType.Type.FLOAT);
        pFontSize.setDefaultValue(24.0);
        ParameterType pLineHeight = addParameterType("lineHeight", ParameterType.Type.FLOAT);
        pLineHeight.setDefaultValue(1.2);
        ParameterType pFillColor = addParameterType("fill", ParameterType.Type.COLOR);
    }

    @Override
    public boolean process(Node node, ProcessingContext ctx) {
        Group g = new Group();

        Text t = new Text(node.asString("text"), node.asFloat("x"), node.asFloat("y"));
        t.setWidth(node.asFloat("width"));
        t.setHeight(node.asFloat("height"));
        t.setFontName(node.asString("fontName"));
        t.setFontSize(node.asFloat("fontSize"));
        t.setLineHeight(node.asFloat("lineHeight"));
        t.setFillColor(node.asColor("fill"));
        g.add(t);
        node.setOutputValue(g);
        return true;
    }

}
