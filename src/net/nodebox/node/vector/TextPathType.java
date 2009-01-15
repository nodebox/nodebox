package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Text;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

public class TextPathType extends PathNodeType {

    public TextPathType(NodeTypeLibrary library) {
        super(library, "textpath");
        setDescription("Creates a text path.");
        ParameterType pText = addParameterType("text", ParameterType.Type.TEXT);
        pText.setDefaultValue("hello");
        ParameterType pX = addParameterType("x", ParameterType.Type.FLOAT);
        ParameterType pY = addParameterType("y", ParameterType.Type.FLOAT);
        ParameterType pWidth = addParameterType("width", ParameterType.Type.FLOAT);
        ParameterType pHeight = addParameterType("height", ParameterType.Type.FLOAT);
        ParameterType pFontName = addParameterType("fontName", ParameterType.Type.FONT);
        pFontName.setDefaultValue("Arial");
        ParameterType pFontSize = addParameterType("fontSize", ParameterType.Type.FLOAT);
        pFontSize.setDefaultValue(24.0);
        ParameterType pLineHeight = addParameterType("lineHeight", ParameterType.Type.FLOAT);
        pLineHeight.setDefaultValue(1.2);
        ParameterType pAlign = addParameterType("align", ParameterType.Type.MENU);
        pAlign.addMenuItem("left", "Left");
        pAlign.addMenuItem("center", "Center");
        pAlign.addMenuItem("right", "Right");
        pAlign.addMenuItem("justify", "Justify");
        pAlign.setDefaultValue("center");
        ParameterType pFillColor = addParameterType("fill", ParameterType.Type.COLOR);
        ParameterType pStrokeColor = addParameterType("stroke", ParameterType.Type.COLOR);
        ParameterType pStrokeWidth = addParameterType("strokewidth", ParameterType.Type.FLOAT);
        pStrokeWidth.setMinimumValue(0.0);
    }

    @Override
    public boolean process(Node node, ProcessingContext ctx) {
        Text t = new Text(node.asString("text"), node.asFloat("x"), node.asFloat("y"));
        t.setWidth(node.asFloat("width"));
        t.setHeight(node.asFloat("height"));
        t.setFontName(node.asString("fontName"));
        t.setFontSize(node.asFloat("fontSize"));
        t.setLineHeight(node.asFloat("lineHeight"));
        t.setAlign(Text.Align.valueOf(node.asString("align").toUpperCase()));
        t.setFillColor(node.asColor("fill"));
        BezierPath p = new BezierPath(t.getPath());
        p.setStrokeColor(node.asColor("stroke"));
        p.setStrokeWidth(node.asFloat("strokewidth"));
        node.setOutputValue(p);
        return true;
    }

}
