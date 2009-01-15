package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Group;
import net.nodebox.graphics.Text;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class TextPathType extends VectorNodeType {

    public TextPathType(NodeTypeLibrary library) {
        super(library, "textpath");
        setDescription("Creates a text path.");
        ParameterType pText = addParameterType("text", ParameterType.Type.TEXT);
        pText.setDefaultValue("hello");
        ParameterType pX = addParameterType("x", ParameterType.Type.FLOAT);
        ParameterType pY = addParameterType("y", ParameterType.Type.FLOAT);
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
        ParameterType pStrokeColor = addParameterType("stroke", ParameterType.Type.COLOR);
        ParameterType pStrokeWidth = addParameterType("strokewidth", ParameterType.Type.FLOAT);
        pStrokeWidth.setMinimumValue(0.0);
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
        BezierPath p = new BezierPath(t.getPath());
        p.setStrokeColor(node.asColor("stroke"));
        p.setStrokeWidth(node.asFloat("strokewidth"));
        g.add(p);
        node.setOutputValue(g);
        return true;
    }

}
