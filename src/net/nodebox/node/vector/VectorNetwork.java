package net.nodebox.node.vector;

import net.nodebox.node.Network;
import net.nodebox.node.Parameter;

public class VectorNetwork extends Network {

    public VectorNetwork() {
        super(Parameter.Type.GROB_VECTOR);
    }

    public VectorNetwork(String name) {
        super(Parameter.Type.GROB_VECTOR, name);
    }
}
