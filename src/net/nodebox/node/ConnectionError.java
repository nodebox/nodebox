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

public class ConnectionError extends RuntimeException {

    private Parameter outputParameter;
    private Parameter inputParameter;

    public ConnectionError(Parameter outputParameter, Parameter inputParameter, String message) {
        super(message);
        this.outputParameter = outputParameter;
        this.inputParameter = inputParameter;
    }

    public Parameter getOutputParameter() {
        return outputParameter;
    }

    public Node getOutputNode() {
        if (outputParameter == null) return null;
        return outputParameter.getNode();
    }

    public Parameter getInputParameter() {
        return inputParameter;
    }

    public Node getInputNode() {
        if (inputParameter == null) return null;
        return inputParameter.getNode();
    }

    @Override
    public String toString() {
        return inputParameter + " => " + outputParameter + ": " + getMessage();
    }
}
