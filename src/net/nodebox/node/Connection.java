/*
 * This file is part of NodeBox.
 *
 * Copyright (C) 2008 Frederik De Bleser (frederik@pandora.be)
 *
 * NodeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NodeBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NodeBox. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nodebox.node;

public class Connection {

    private Parameter output;
    private Parameter input;

    public Connection(Parameter output, Parameter input) {
        assert (output.isOutputParameter());
        assert (input.isInputParameter());
        this.output = output;
        this.input = input;
    }

    public Parameter getInputParameter() {
        return input;
    }

    public Parameter getOutputParameter() {
        return output;
    }

    public Node getInputNode() {
        return input.getNode();
    }

    public Node getOutputNode() {
        if (!hasOutput()) {
            return null;
        }
        return output.getNode();
    }

    public boolean hasOutput() {
        return output != null;
    }

    private void markDirtyDownstream() {
        getInputNode().markDirty();
    }

    private void update() {
        // TODO: implement
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
