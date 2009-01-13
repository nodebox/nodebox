package net.nodebox.node.vector;

import net.nodebox.graphics.Grob;
import net.nodebox.graphics.Group;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

import java.util.List;

public class MergeType extends VectorNodeType {

    public MergeType(NodeTypeLibrary library) {
        super(library, "merge");
        setDescription("Merges multiple vector nodes together.");
        ParameterType pShapes = addParameterType("shapes", ParameterType.Type.GROB_VECTOR);
        pShapes.setCardinality(ParameterType.Cardinality.MULTIPLE);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        Group outputGroup = new Group();
        List<Object> shapes = node.getValues("shapes");
        for (Object shapeObject : shapes) {
            Grob shape = (Grob) shapeObject;
            outputGroup.add(shape.clone());
        }
        node.setOutputValue(outputGroup);
        return true;
    }

}
