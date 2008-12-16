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

import net.nodebox.graphics.Color;
import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.UnresolveablePropertyException;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.impl.BaseVariableResolverFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
        ProxyResolverFactory prf = new ProxyResolverFactory(parameter.getNode());
        try {
            return MVEL.executeExpression(compiledExpression, prf);
        } catch (Exception e) {
            throw new RuntimeException("Error with expression '" + expression + "' on " + getParameter().getAbsolutePath(), e);
        }
    }

    List<Parameter> getDependentParameters() {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    class ProxyResolverFactory extends BaseVariableResolverFactory {

        private Node node;
        private NodeAccessProxy proxy;

        ProxyResolverFactory(Node node) {
            this.node = node;
            proxy = new NodeAccessProxy(node);
        }

        public Node getNode() {
            return node;
        }

        public NodeAccessProxy getProxy() {
            return proxy;
        }

        public VariableResolver createVariable(String name, Object value) {
            throw new CompileException("Variable assignment is not supported.");
        }

        public VariableResolver createVariable(String name, Object value, Class<?> type) {
            throw new CompileException("Variable assignment is not supported.");
        }

        @Override
        public VariableResolver getVariableResolver(String name) {
            if (variableResolvers == null) variableResolvers = new HashMap<String, VariableResolver>();
            VariableResolver vr = variableResolvers.get(name);
            if (vr != null) {
                return vr;
            } else if (proxy.containsKey(name)) {
                vr = new ProxyResolver(proxy, proxy.get(name));
                variableResolvers.put(name, vr);
                return vr;
            } else if (nextFactory != null) {
                return nextFactory.getVariableResolver(name);
            }
            throw new UnresolveablePropertyException("unable to resolve variable '" + name + "'");
        }

        public boolean isResolveable(String name) {
            return (variableResolvers != null && variableResolvers.containsKey(name))
                    || (proxy.containsKey(name))
                    || (nextFactory != null && nextFactory.isResolveable(name));
        }

        public boolean isTarget(String name) {
            return variableResolvers != null && variableResolvers.containsKey(name);
        }

        @Override
        public Set<String> getKnownVariables() {
            return proxy.keySet();
        }

    }

    class ProxyResolver implements VariableResolver {

        private NodeAccessProxy proxy;
        private Object value;

        ProxyResolver(NodeAccessProxy proxy, Object value) {
            this.proxy = proxy;
            this.value = value;
        }

        public NodeAccessProxy getProxy() {
            return proxy;
        }

        public Node getNode() {
            return proxy.getNode();
        }

        public String getName() {
            return proxy.getNode().getName();
        }

        public Class getType() {
            return Object.class;
        }

        public void setStaticType(Class type) {
            throw new RuntimeException("Not implemented");
        }

        public int getFlags() {
            return 0;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            throw new CompileException("Parameter values cannot be changed through expressions.");
        }
    }

}
