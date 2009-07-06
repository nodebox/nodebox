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
package nodebox.node;

import nodebox.graphics.Color;
import nodebox.node.ExpressionHelper;
import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.UnresolveablePropertyException;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.impl.BaseVariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Expression {

    static ParserContext parserContext = new ParserContext();

    static {
        // Initialize MVEL.

        // The dynamic optimizer crashes for some reason, so we use the "safe reflective" one.
        // Although "safe" sounds slower, this optimizer actually seems *faster*
        // than the dynamic one. Don't change this unless you want to go digging for weird
        // reflective constructor errors.
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);

        // Add "built-in" methods to the expression context.
        parserContext = new ParserContext();
        try {
            // MVEL has a bug where it accepts methods with varargs, but only executes the method with
            // non-varargs. So in our ExpressionHelper we have both the varargs and non-varargs methods.
            // We lookup the varargs version here, but only the non-varargs will get called.
            parserContext.addImport("random", ExpressionHelper.class.getMethod("random", long.class, double[].class));
            parserContext.addImport("color", ExpressionHelper.class.getMethod("color", double[].class));
            parserContext.addImport("stamp", ExpressionHelper.class.getMethod("stamp", String.class, Object.class));
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Unknown static method for expression." + e);
        }
    }

    private Parameter parameter;
    private String expression = "";
    private transient Serializable compiledExpression;
    private Set<WeakReference<Parameter>> markedParameterReferences;

    public Expression(Parameter parameter, String expression) {
        assert parameter != null; // We need the current parameter for stamp expressions.
        this.parameter = parameter;
        setExpression(expression);
    }

    //// Attribute access ////

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) throws ExpressionError {
        if (this.expression != null && this.expression.equals(expression)) return;
        this.expression = expression;
        markedParameterReferences = null;
        ExpressionCompiler compiler = new ExpressionCompiler(expression);
        try {
            this.compiledExpression = compiler.compile(parserContext);
        } catch (Exception e) {
            throw new ExpressionError("Error with expression '" + expression + "' on " + getParameter().getAbsolutePath(), e);
        }
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
            throw new IllegalArgumentException("Value \"" + value + "\" for expression \"" + expression + "\" is not an integer.");
        }
    }

    public double asFloat() {
        Object value = evaluate();
        if (value instanceof Number) {
            return (Double) value;
        } else {
            throw new IllegalArgumentException("Value \"" + value + "\" for expression \"" + expression + "\" is not a floating-point value.");
        }
    }

    public String asString() {
        Object value = evaluate();
        if (value instanceof String) {
            return (String) value;
        } else {
            throw new IllegalArgumentException("Value \"" + value + "\" for expression \"" + expression + "\" is not a string.");
        }
    }

    public Color asColor() {
        Object value = evaluate();
        if (value instanceof Color) {
            return (Color) value;
        } else {
            throw new IllegalArgumentException("Value \"" + value + "\" for expression \"" + expression + "\" is not a color.");
        }
    }

    //// Evaluation ////

    /**
     * Evaluate the expression and return the result.
     *
     * @return the result of the expression
     * @throws ExpressionError if an error occurs whilst evaluating the expression.
     */
    public Object evaluate() throws ExpressionError {
        return evaluate(new ProcessingContext());
    }

    /**
     * Evaluate the expression and return the result.
     *
     * @param context the context wherein evaluation happens.
     * @return the result of the expression
     * @throws ExpressionError if an error occurs whilst evaluating the expression.
     */
    public Object evaluate(ProcessingContext context) throws ExpressionError {
        // Set up state variables in the expression utilities class.
        // This is not thread-safe.
        ExpressionHelper.currentContext = context;
        ExpressionHelper.currentParameter = parameter;
        // Marked parameter references are used to find which parameters this expression references.
        markedParameterReferences = new HashSet<WeakReference<Parameter>>();
        ProxyResolverFactory prf = new ProxyResolverFactory(parameter.getNode(), context, markedParameterReferences);
        try {
            return MVEL.executeExpression(compiledExpression, prf);
        } catch (Exception e) {
            throw new ExpressionError("Error with expression '" + expression + "' on " + getParameter().getAbsolutePath(), e);
        }
    }

    /**
     * Returns all parameters this expression depends on
     * <p/>
     * If the expression contains an error, this method will return an empty set.
     *
     * @return a set of parameters
     */
    public Set<Parameter> getDependencies() {
        if (markedParameterReferences == null) {
            try {
                evaluate();
            } catch (ExpressionError expressionError) {
                return new HashSet<Parameter>(0);
            }
        }
        HashSet<Parameter> dependencies = new HashSet<Parameter>(markedParameterReferences.size());
        for (WeakReference<Parameter> ref : markedParameterReferences) {
            Parameter p = ref.get();
            if (p != null)
            dependencies.add(p);
        }
        return dependencies;
    }

    class ProxyResolverFactory extends BaseVariableResolverFactory {

        private Node node;
        private NodeAccessProxy proxy;
        private ProcessingContext context;

        public ProxyResolverFactory(Node node, ProcessingContext context) {
            this.node = node;
            proxy = new NodeAccessProxy(node);
            this.context = context;
        }

        public ProxyResolverFactory(Node node, ProcessingContext context, Set<WeakReference<Parameter>> markedParameterReferences) {
            this.node = node;
            proxy = new NodeAccessProxy(node, markedParameterReferences);
            this.context = context;
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
            } else if (context.containsKey(name)) {
                vr = new ProcessingContextResolver(context, name);
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
                    || (context.containsKey(name))
                    || (nextFactory != null && nextFactory.isResolveable(name));
        }

        public boolean isTarget(String name) {
            return variableResolvers != null && variableResolvers.containsKey(name);
        }

        @Override
        public Set<String> getKnownVariables() {
            Set<String> knownVariables = new HashSet<String>();
            knownVariables.addAll(proxy.keySet());
            knownVariables.addAll(context.keySet());
            return knownVariables;
        }
    }

    class ProxyResolver implements VariableResolver {

        private NodeAccessProxy proxy;
        private Object value;

        public ProxyResolver(NodeAccessProxy proxy, Object value) {
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

    class ProcessingContextResolver implements VariableResolver {

        private String name;
        private ProcessingContext context;

        ProcessingContextResolver(ProcessingContext context, String name) {
            this.context = context;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Class getType() {
            return Object.class;
        }

        public void setStaticType(Class aClass) {
            throw new RuntimeException("Not implemented");
        }

        public int getFlags() {
            return 0;
        }

        public Object getValue() {
            return context.get(name);
        }

        public void setValue(Object o) {
            throw new CompileException("You cannot change the value of a constant.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Expression that = (Expression) o;

        if (!expression.equals(that.expression)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }
}
