package net.nodebox.node;

import java.util.ArrayList;
import java.util.List;

public class MultiConnection extends Connection {
    private List<Parameter> outputParameters;

    public MultiConnection(Parameter outputParameter, Parameter inputParameter) {
        super(inputParameter, outputParameter);
        this.outputParameters = new ArrayList<Parameter>();
        this.outputParameters.add(outputParameter);
    }


}
