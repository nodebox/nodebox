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

import org.mvel.MVEL;
import org.mvel.PreProcessor;
import org.mvel.CompiledExpression;

import java.util.Collection;
import java.util.List;
import java.io.Serializable;

import net.nodebox.graphics.Color;

public class Expression {

    private Parameter parameter;
    private String expression = "";
    private transient Serializable compiledExpression;

    public Expression(Parameter parameter, String expression) {
        this.parameter = parameter;
        setExpression(expression);
    }
    //// Attribute access ////

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        if (this.expression.equals(expression)) {
            return;
        }
        this.expression = expression;
        this.compiledExpression = MVEL.compileExpression(expression);
        parameter.getNode().markDirty();
    }

    public Parameter getParameter() {
        return parameter;
    }

    //// Values ////

    public int asInt() {
        Object value = evaluate();
        if (value instanceof Number) {
            return (Integer) value;
        } else {
            throw new ValueError("Value \"" + value + "\" for expression \"" + expression + "\" is not an integer.");
        }
    }

    public double asFloat() {
        Object value = evaluate();
        if (value instanceof Number) {
            return (Double) value;
        } else {
            throw new ValueError("Value \"" + value + "\" for expression \"" + expression + "\" is not a floating-point value.");
        }
    }

    public String asString() {
        Object value = evaluate();
        if (value instanceof String) {
            return (String) value;
        } else {
            throw new ValueError("Value \"" + value + "\" for expression \"" + expression + "\" is not a string.");
        }
    }

    public Color asColor() {
        Object value = evaluate();
        if (value instanceof Color) {
            return (Color) value;
        } else {
            throw new ValueError("Value \"" + value + "\" for expression \"" + expression + "\" is not a color.");
        }
    }

    //// Evaluation ////

    public Object evaluate() {
        return MVEL.executeExpression(compiledExpression);
    }

    List<Parameter> getDependentParameters() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
