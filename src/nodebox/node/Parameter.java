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
import nodebox.util.StringUtils;

import java.util.*;

/**
 * A parameter controls the operation of a Node. It provide an interface into the workings of a node and allows a user
 * to change its behaviour. Parameters are represented by standard user interface controls, such as sliders for numbers,
 * text fields for strings, and checkboxes for booleans.
 * <p/>
 * Parameters implement the observer pattern for expressions. Parameters that are dependent on other parameters because
 * of their expressions will observe the parameters they depend on, and marked the node as dirty whenever they receive
 * an update event from one of the parameters they depend on.
 */
public class Parameter {

    /**
     * The primitive type of a parameter. This is different from the control UI that is used to represent this parameter.
     */
    public enum Type {
        /**
         * An integer value
         */
        INT,

        /**
         * A floating-point value
         */
        FLOAT,

        /**
         * A string value
         */
        STRING,

        /**
         * A color
         */
        COLOR,

        /**
         * Executable code
         */
        CODE
    }

    /**
     * The UI control for this parameter. This defines how the parameter is represented in the user interface.
     */
    public enum Widget {
        ANGLE, COLOR, FILE, FLOAT, FONT, GRADIENT, IMAGE, INT, MENU, SEED, STRING, TEXT, TOGGLE, NODEREF, STAMP_EXPRESSION, CODE
    }

    /**
     * The way in which values will be bound to a minimum and maximum value. Only hard bounding enforces the
     * minimum and maximum value.
     */
    public enum BoundingMethod {
        NONE, SOFT, HARD
    }

    /**
     * The steps where this parameter will be shown. If it is hidden, it will not be shown anywhere.
     * If it is in the detail view, it will not show up in the HUD. The HUD is the highest level; this
     * means that the control will be shown everywhere.
     */
    public enum DisplayLevel {
        HIDDEN, DETAIL, HUD
    }

    public static class MenuItem {
        private String key;
        private String label;

        public MenuItem(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MenuItem menuItem = (MenuItem) o;

            if (!key.equals(menuItem.key)) return false;
            if (!label.equals(menuItem.label)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = key.hashCode();
            result = 31 * result + label.hashCode();
            return result;
        }
    }

    public static final HashMap<Type, Class> TYPE_MAPPING;
    public static final HashMap<Type, Widget> WIDGET_MAPPING;
    public static final HashMap<Widget, Type> REVERSE_WIDGET_MAPPING;
    public static final NodeCode emptyCode = new EmptyCode();

    static {
        TYPE_MAPPING = new HashMap<Type, Class>();
        TYPE_MAPPING.put(Type.INT, Integer.class);
        TYPE_MAPPING.put(Type.FLOAT, Float.class);
        TYPE_MAPPING.put(Type.STRING, String.class);
        TYPE_MAPPING.put(Type.COLOR, Color.class);
        TYPE_MAPPING.put(Type.CODE, NodeCode.class);
        WIDGET_MAPPING = new HashMap<Type, Widget>();
        WIDGET_MAPPING.put(Type.INT, Widget.INT);
        WIDGET_MAPPING.put(Type.FLOAT, Widget.FLOAT);
        WIDGET_MAPPING.put(Type.STRING, Widget.STRING);
        WIDGET_MAPPING.put(Type.COLOR, Widget.COLOR);
        WIDGET_MAPPING.put(Type.CODE, Widget.CODE);
        REVERSE_WIDGET_MAPPING = new HashMap<Widget, Type>();
        REVERSE_WIDGET_MAPPING.put(Widget.ANGLE, Type.FLOAT);
        REVERSE_WIDGET_MAPPING.put(Widget.COLOR, Type.COLOR);
        REVERSE_WIDGET_MAPPING.put(Widget.FILE, Type.STRING);
        REVERSE_WIDGET_MAPPING.put(Widget.FLOAT, Type.FLOAT);
        REVERSE_WIDGET_MAPPING.put(Widget.FONT, Type.STRING);
        REVERSE_WIDGET_MAPPING.put(Widget.GRADIENT, Type.STRING);
        REVERSE_WIDGET_MAPPING.put(Widget.IMAGE, Type.STRING);
        REVERSE_WIDGET_MAPPING.put(Widget.INT, Type.INT);
        REVERSE_WIDGET_MAPPING.put(Widget.MENU, Type.STRING);
        REVERSE_WIDGET_MAPPING.put(Widget.SEED, Type.INT);
        REVERSE_WIDGET_MAPPING.put(Widget.STRING, Type.STRING);
        REVERSE_WIDGET_MAPPING.put(Widget.TEXT, Type.STRING);
        REVERSE_WIDGET_MAPPING.put(Widget.TOGGLE, Type.INT);
        REVERSE_WIDGET_MAPPING.put(Widget.NODEREF, Type.STRING);
        REVERSE_WIDGET_MAPPING.put(Widget.STAMP_EXPRESSION, Type.STRING);
        REVERSE_WIDGET_MAPPING.put(Widget.CODE, Type.CODE);
    }

    private Node node;
    private String name;
    private String label;
    private String helpText;
    private Type type;
    private Widget widget;
    private Object value;
    private Expression expression;
    private BoundingMethod boundingMethod = BoundingMethod.NONE;
    private Float minimumValue, maximumValue; // Objects, because they can be null.
    private DisplayLevel displayLevel = DisplayLevel.HUD;
    private Expression enableExpression;
    private List<MenuItem> menuItems = new ArrayList<MenuItem>(0);
    private transient boolean dirty;
    private transient boolean hasStampExpression;

    public Parameter(Node node, String name, Type type) {
        this.node = node;
        // Type needs to come first, because validateName can cause toString() to happen which requires the name.
        this.type = type;
        validateName(name);
        this.name = name;
        this.widget = getDefaultWidget(type);
        this.label = StringUtils.humanizeName(name);
        revertToDefault();
    }

    //// Basic operations ////

    public Node getNode() {
        return node;
    }

    public NodeLibrary getLibrary() {
        return node.getLibrary();
    }

    public Parameter getPrototype() {
        Node pn = node.getPrototype();
        if (pn == null) return null;
        return pn.getParameter(name);
    }

    //// Naming ////

    public String getName() {
        return name;
    }

    public void setName(String name) throws InvalidNameException {
        if (name != null && getName().equals(name)) return;
        validateName(name);
        String oldName = this.name;
        this.name = name;
        node.renameParameter(this, oldName, name);
        fireAttributeChanged();
    }

    public void validateName(String name) {
        if (name == null || name.trim().length() == 0)
            throw new InvalidNameException(this, name, "Name cannot be null or empty.");
        if (node.hasParameter(name))
            throw new InvalidNameException(this, name, "There is already a parameter named " + name + ".");
        if (node.hasPort(name))
            throw new InvalidNameException(this, name, "There is already a port named " + name + ".");
        // Use the same validation as for nodes.
        Node.validateName(name);
    }

    public String getAbsolutePath() {
        return getNode().getAbsolutePath() + "/" + getName();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        if (label == null || label.trim().length() == 0)
            this.label = StringUtils.humanizeName(name);
        else
            this.label = label;
        fireAttributeChanged();
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
        fireAttributeChanged();
    }

    //// Type ////

    public Type getType() {
        return type;
    }

    /**
     * Change the type of this parameter.
     * <p/>
     * The existing value will be migrated to the new type. Changing the type to code will
     * not try to parse the value, since this can given unexpected results.
     * <p/>
     * The new value will be clamped to bounds (if bounding method is set to hard), and
     * the widget will be set to the default widget for this type.
     *
     * @param newType the new type
     */
    public void setType(Type newType) {
        if (this.type == newType) return;
        // Try to migrate the value to the new type
        if (hasExpression()) {
            // Do nothing. It is too hard to change expressions to return a value of the new type.
        } else {
            try {
                value = parseValue(asString(), newType);
            } catch (IllegalArgumentException e) {
                // If the value could not be parsed, reset it to the default value.
                value = getDefaultValue(newType);
            }
        }
        this.type = newType;
        clampToBounds();
        // The old widget most likely doesn't make any sense for the new type.
        this.widget = getDefaultWidget(newType);
        fireAttributeChanged();
    }

    //// Widget ////

    public Widget getWidget() {
        return widget;
    }

    public void setWidget(Widget widget) {
        if (this.widget == widget) return;
        // Changing the widget mostly means changing the type.
        Type oldType = getTypeForWidget(this.widget);
        Type newType = getTypeForWidget(widget);
        // If the old and new type are the same, we don't need to migrate the type.
        if (oldType != newType) {
            // Setting the type will change the widget to the default widget, so the widget
            // will be set *after* the type is migrated.
            setType(newType);
        }
        this.widget = widget;
        fireAttributeChanged();
    }

    public static Widget getDefaultWidget(Type type) {
        return WIDGET_MAPPING.get(type);
    }

    public static Type getTypeForWidget(Widget widget) {
        return REVERSE_WIDGET_MAPPING.get(widget);
    }

    //// Bounding ////

    public BoundingMethod getBoundingMethod() {
        return boundingMethod;
    }

    public void setBoundingMethod(BoundingMethod boundingMethod) {
        if (this.boundingMethod == boundingMethod) return;
        this.boundingMethod = boundingMethod;
        if (boundingMethod == BoundingMethod.HARD)
            clampToBounds();
        fireAttributeChanged();
    }

    public Float getMinimumValue() {
        return minimumValue;
    }

    public void setMinimumValue(Float minimumValue) {
        if (this.minimumValue != null && this.minimumValue.equals(minimumValue)) return;
        if (minimumValue != null && this.maximumValue != null && minimumValue > this.maximumValue)
            minimumValue = maximumValue;
        this.minimumValue = minimumValue;
        if (boundingMethod == BoundingMethod.HARD)
            clampToBounds();
        fireAttributeChanged();
    }

    public Float getMaximumValue() {
        return maximumValue;
    }

    public void setMaximumValue(Float maximumValue) {
        if (this.maximumValue != null && this.maximumValue.equals(maximumValue)) return;
        if (maximumValue != null && this.minimumValue != null && maximumValue < this.minimumValue)
            maximumValue = minimumValue;
        this.maximumValue = maximumValue;
        if (boundingMethod == BoundingMethod.HARD)
            clampToBounds();
        fireAttributeChanged();
    }

    private void clampToBounds() {
        if (type == Type.INT) {
            int v = (Integer) value;
            if (minimumValue != null && v < minimumValue) {
                set(minimumValue.intValue());
            } else if (maximumValue != null && v > maximumValue) {
                set(maximumValue.intValue());
            }
        } else if (type == Type.FLOAT) {
            float v = (Float) value;
            if (minimumValue != null && v < minimumValue) {
                set(minimumValue);
            } else if (maximumValue != null && v > maximumValue) {
                set(maximumValue);
            }
        }
    }

    //// Display level ////

    public DisplayLevel getDisplayLevel() {
        return displayLevel;
    }

    public void setDisplayLevel(DisplayLevel displayLevel) {
        this.displayLevel = displayLevel;
        fireAttributeChanged();
    }

    public void fireAttributeChanged() {
        getLibrary().fireNodeAttributeChanged(node, Node.Attribute.PARAMETER);
    }

    //// Enable expressions ////

    /**
     * Check if the parameter is enabled.
     * <p/>
     * This evaluates the enable expression.
     * The enable flag has no effect on the behaviour of Parameter: you can still set/get values, change metadata, etc.
     * It is the UI's responsibility to react on the enable flag.
     * <p/>
     * The enabled state is not cached and the disable expression is evaluated every time.
     *
     * @return true if this parameter is enabled, has no expression or the expression has an error.
     */
    public boolean isEnabled() {
        if (enableExpression == null) return true;
        try {
            return enableExpression.asBoolean();
        } catch (ExpressionError expressionError) {
            return true;
        }
    }

    /**
     * Set the expression used for determining if the parameter is disabled.
     * <p/>
     * Calling isDisabled will now evaluate this expression every time.
     *
     * @param expression the disable expression.
     * @see #isEnabled()
     */
    public void setEnableExpression(String expression) {
        if (expression == null || expression.trim().length() == 0) {
            if (enableExpression == null) return;
            enableExpression = null;
        } else if (enableExpression != null && enableExpression.getExpression().equals(expression)) {
            return;
        } else {
            enableExpression = new Expression(this, expression);
        }
        fireAttributeChanged();
    }

    /**
     * Get the disable expression used for determining if the parameter is disabled.
     *
     * @return the disable expression.
     */
    public String getEnableExpression() {
        if (enableExpression == null) return "";
        return enableExpression.getExpression();
    }

    public boolean hasEnableExpressionError() {
        return enableExpression != null && enableExpression.getError() != null;
    }

    public Throwable getEnableExpressionError() {
        if (enableExpression == null) return null;
        return enableExpression.getError();
    }

    //// Menu items ////

    public List<MenuItem> getMenuItems() {
        return menuItems;
    }

    public void addMenuItem(String key, String label) {
        menuItems.add(new MenuItem(key, label));
        fireAttributeChanged();
    }

    public void removeMenuItem(String key) {
        MenuItem itemToRemove = null;
        for (MenuItem item : menuItems) {
            if (item.getKey().equals(key)) {
                itemToRemove = item;
                break;
            }
        }
        if (itemToRemove == null) return;
        menuItems.remove(itemToRemove);
        fireAttributeChanged();
    }

    public void removeMenuItem(MenuItem item) {
        menuItems.remove(item);
        fireAttributeChanged();
    }

    //// Values ////

    public int asInt() {
        if (type == Type.INT) {
            return (Integer) value;
        } else if (type == Type.FLOAT) {
            float v = (Float) value;
            return (int) v;
        } else {
            return 0;
        }
    }

    public float asFloat() {
        if (type == Type.INT) {
            int v = (Integer) value;
            return (float) v;
        } else if (type == Type.FLOAT) {
            return (Float) value;
        } else {
            return 0;
        }
    }

    public String asString() {
        if (value == null) return null;
        if (type == Type.STRING) {
            return (String) value;
        } else if (type == Type.CODE) {
            return ((NodeCode) value).getSource();
        } else {
            return value.toString();
        }
    }

    public boolean asBoolean() {
        if (type == Type.INT) {
            int v = (Integer) value;
            return v == 1;
        } else {
            return false;
        }
    }

    public Color asColor() {
        if (type == Type.COLOR) {
            return (Color) getValue();
        } else {
            return new Color();
        }
    }

    public NodeCode asCode() {
        if (type == Type.CODE) {
            return (NodeCode) getValue();
        } else {
            return null;
        }
    }

    public String asExpression() {
        if (type == Type.INT) {
            return String.valueOf((Integer) value);
        } else if (type == Type.FLOAT) {
            return String.valueOf((Float) value);
        } else if (type == Type.STRING) {
            String v = (String) value;
            // Quote the string
            v = v.replaceAll("\"", "\\\"");
            return "\"" + v + "\"";
        } else if (type == Type.COLOR) {
            Color v = (Color) value;
            return String.format(Locale.US, "color(%.2f, %.2f, %.2f, %.2f)", v.getRed(), v.getGreen(), v.getBlue(), v.getAlpha());
        } else if (type == Type.CODE) {
            return ((NodeCode) value).getSource();
        } else {
            throw new AssertionError("Cannot convert parameter value " + asString() + " of type " + getType() + " to expression.");
        }
    }

    /**
     * Returns the value of this node. This is a safe copy and you can modify it at will.
     * <p/>
     * Only Color objects are cloned. The other value types are immutable, so they do not need to be cloned.
     *
     * @return a clone of the original value.
     */
    public Object getValue() {
        if (value instanceof Color) {
            return ((Color) value).clone();
        } else {
            return value;
        }
    }

    public void set(Object value) throws IllegalArgumentException {
        setValue(value);
    }

    public void setValue(Object value) throws IllegalArgumentException {
        if (hasExpression()) {
            throw new IllegalArgumentException("The parameter has an expression set.");
        }
        // validate throws IllegalArgumentException when the value fails validation.
        validate(value);

        // As a special exception, integer values can be cast up to floating-point values,
        // and double values can be cast down (losing precision).
        Object castValue;
        if (value instanceof Integer && type == Type.FLOAT) {
            castValue = (float) ((Integer) value);
        } else if (value instanceof Double && type == Type.FLOAT) {
            castValue = (float) ((Double) value).doubleValue();
        } else {
            castValue = value;
        }
        if (this.value != null && this.value.equals(castValue)) return;

        this.value = castValue;
        markDirty();
    }

    /**
     * Mark this parameter and its node as dirty. Also notify dependent parameters.
     */
    /* package private */ void markDirty() {
        if (dirty) return;
        dirty = true;
        fireValueChanged();
    }

    //// Validation ////

    public void validate(Object value) throws IllegalArgumentException {
        // Check null
        if (value == null)
            throw new IllegalArgumentException("Value for parameter " + getName() + " cannot be null.");
        // Check if the type matches
        switch (type) {
            case INT:
                if (!(value instanceof Integer))
                    throw new IllegalArgumentException("Value is not an int.");
                break;
            case FLOAT:
                // As a special exception, we accept integer and double values for float type parameters.
                if (!(value instanceof Float || value instanceof Double || value instanceof Integer))
                    throw new IllegalArgumentException("Value is not a float.");
                break;
            case STRING:
                if (!(value instanceof String))
                    throw new IllegalArgumentException("Value is not a String.");
                break;
            case COLOR:
                if (!(value instanceof Color))
                    throw new IllegalArgumentException("Value is not a Color.");
                break;
            case CODE:
                if (!(value instanceof NodeCode))
                    throw new IllegalArgumentException("Value is not a NodeCode object.");
                break;
        }
        // If hard bounds are set, check if the value falls within the bounds.
        if (getBoundingMethod() == BoundingMethod.HARD) {
            float floatValue;
            if (value instanceof Integer) {
                floatValue = (Integer) value;
            } else if (value instanceof Float) {
                floatValue = (Float) value;
            } else if (value instanceof Double) {
                floatValue = (float) ((Double) value).doubleValue();
            } else {
                throw new AssertionError("Bounding set, but value is not integer or float. (type: " + this + " value: " + value + ")");
            }
            if (minimumValue != null && floatValue < minimumValue) {
                throw new IllegalArgumentException("Parameter " + getName() + ": value " + value + " is too small. (minimum=" + minimumValue + ")");
            }
            if (maximumValue != null && floatValue > maximumValue) {
                throw new IllegalArgumentException("Parameter " + getName() + ": value " + value + " is too big. (maximum=" + maximumValue + ")");
            }
        }
    }

    //// Expressions ////

    public boolean hasExpression() {
        return expression != null;
    }

    public boolean hasExpressionError() {
        return hasExpression() && expression.hasError();
    }

    public Throwable getExpressionError() {
        return hasExpression() ? expression.getError() : null;
    }

    public String getExpression() {
        return hasExpression() ? expression.getExpression() : "";
    }

    public void clearExpression() {
        this.expression = null;
        hasStampExpression = false;
        removeDependencies();
        markDirty();
    }

    /**
     * Set the expression to the given value.
     *
     * @param expression the expression, in MVEL format.
     * @return false if the expression could not be evaluated.
     */
    public boolean setExpression(String expression) {
        // We used to check if the expression was equal to the given expression, but this causes problems
        // when new parameters are added that are relevant to the expression, i.e. Parameter "a" refers to "b" but
        // parameter "b" does not exist yet. The expression becomes valid the moment we add "b", but to make this
        // happen, we need to set "a" again to the same expression.
        // TODO: This is more of a temporary workaround than a final solution.
        // Ideally, the system should detect that the expression becomes valid because a new parameter was created.
        // However, this means we can no longer use MVELs dependency detection.
        if (expression == null || expression.trim().length() == 0) {
            clearExpression();
            return true;
        }
        // Remove the dependencies first in case creating the expression throws an error.
        removeDependencies();
        // Set the new expression.
        this.expression = new Expression(this, expression);
        // Reset the stamp flag. It will be set by markStampExpression(), which will be called
        // from the expression helper while evaluating the expression.
        hasStampExpression = false;
        // Evaluate the expression to see if it returns any errors.
        try {
            this.expression.evaluate();
        } catch (ExpressionError ignored) {
            // Note that we catch the error, but do not handle it.
            // We want to be able to work with erroneous expressions, and only have the error
            // happen when the Node is updated, updating parameters and thus expressions.
            // We simply return false to indicate that the method has an error.
            // You can call hasExpressionError to check if the expression is faulty.
            // Note that some expressions can become faulty at runtime, due to the dynamic nature of code.

            // Even when an expression fails, the parameter is still marked dirty, since we want to update the
            // node as soon as possible to inform the user of the error.
            markDirty();
            return false;
        }
        // Setting an expression automatically enables it and marks the parameter as dirty.
        markDirty();
        try {
            updateDependencies();
        } catch (IllegalArgumentException e) {
            // Whilst updating, we might catch a Connection error meaning you are connecting
            // e.g. the parameter to itself. If that happens, we clear out the expression and all of its
            // dependencies.
            removeDependencies();
            this.expression.setError(e);
            return false;
        }
        return true;
    }

    /**
     * Check if the parameter has an expression containing the stamp function.
     *
     * @return true if the parameter has a stamp expression.
     */
    public boolean hasStampExpression() {
        return hasStampExpression;
    }

    /**
     * Marks this parameter as using the stamp expression.
     * <p/>
     * Do not call this method yourself. This method is only used by ExpressionUtils.stamp() to indicate
     * that the stamp expression was used.
     */
    /* package private */ void markStampExpression() {
        this.hasStampExpression = true;
    }

    //// Expression dependencies ////

    /**
     * The parameter dependencies function like a directed-acyclic graph, just like the node framework itself.
     * Parameter dependencies are created by setting expressions that refer to other parameters. Once these parameters
     * are changed, the dependent parameters need to be changed as well.
     *
     * @throws DependencyError when there is an error creating the dependencies.
     */
    private void updateDependencies() throws DependencyError {
        removeDependencies();
        for (Parameter p : expression.getDependencies()) {
            // Add the parameter I depend on to as a dependency.
            // This also makes the reverse connection in the dependency graph.
            addDependency(p);
        }
    }

    /**
     * Add the given parameter as a dependency.
     * <p/>
     * This means that whenever this parameter needs to be updated, it needs to update
     * the given parameter.
     *
     * @param p the parameter this node depends on.
     */
    private void addDependency(Parameter p) {
        getLibrary().addParameterDependency(p, this);
    }

    /**
     * This method gets called whenever the expression was cleared. It removes all dependencies
     * for this parameters.
     * <p/>
     * The dependents (parameters that rely on this parameter) are not changed. They only change
     * when their dependencies are cleared.
     */
    private void removeDependencies() {
        getLibrary().removeParameterDependencies(this);
    }

    /**
     * This method gets called when the parameter is about to be removed. It signal all of its dependent nodes
     * that the parameter will no longer be available.
     * <p/>
     * The dependent parameters will probably all have invalid expressions from now on.
     */
    private void removeDependents() {
        // Before removing all dependents, inform them first of the fact that one of their dependencies has changed.
        for (Parameter p : getDependents()) {
            p.dependencyChangedEvent();
        }
        getLibrary().removeParameterDependents(this);
    }

    /**
     * Get all parameters that rely on this parameter.
     * <p/>
     * These parameters all have expressions that point to this parameter. Whenever this parameter changes,
     * they get notified.
     * <p/>
     * This list contains all "live" parameters when you call it. Please don't hold on to this list for too long,
     * since parameters can be added and removed at will.
     *
     * @return a list of parameters that depend on this parameter. This list can safely be modified.
     */
    public Set<Parameter> getDependents() {
        return getLibrary().getParameterDependents(this);
    }

    /**
     * Get all parameters this parameter depends on.
     * <p/>
     * This list contains all "live" parameters when you call it. Please don't hold on to this list for too long,
     * since parameters can be added and removed at will.
     *
     * @return a list of parameters this parameter depends on. This list can safely be modified.
     */
    public Set<Parameter> getDependencies() {
        return getLibrary().getParameterDependencies(this);
    }

    /**
     * Check if this parameter depends on the given parameter.
     *
     * @param other the possibly dependent parameter.
     * @return true if this parameter depends on the given parameter.
     */
    public boolean dependsOn(Parameter other) {
        return getLibrary().getParameterDependencies(this).contains(other);
    }

    /**
     * Called whenever the value of this parameter changes. This method informs the dependent parameters that my value
     * has changed.
     */
    protected void fireValueChanged() {
        getLibrary().fireValueChanged(getNode(), this);
        getNode().markDirty();
        for (Parameter p : getDependents()) {
            p.dependencyChangedEvent();
        }
    }

    private void dependencyChangedEvent() {
        markDirty();
    }

    /**
     * This event happens when the parameter is about to be removed.
     * <p/>
     * We remove all dependencies/dependents here.
     */
    public void removedEvent() {
        removeDependencies();
        removeDependents();
    }

    /**
     * Updates the parameter, making sure all dependencies are clean.
     * <p/>
     * This method can take a long time and should be run in a separate thread.
     *
     * @param context the processing context
     * @throws ExpressionError if an expression fails
     */
    public void update(ProcessingContext context) throws ExpressionError {
        if (!dirty) return;
        context.setNode(node);
        // To avoid infinite recursion, we set dirty to false before processing
        // any of the dependencies. If we come by this parameter again, we have
        // already updated it.
        dirty = false;
        if (hasExpression()) {
            // Update all dependencies.
            for (Parameter p : getDependencies()) {
                p.update(context);
            }

            Object expressionValue = expression.evaluate(context);
            expressionValue = convertToType(expressionValue);
            validate(expressionValue);
            value = expressionValue;
            fireValueChanged();
        }
    }

    /**
     * Convert the given value to the correct type for this parameter.
     * <p/>
     * This method assumes the value has already been validated, and is only used to convert int and doubles
     * to their correct float representation.
     *
     * @param value a value
     * @return the unchanged object, or the value converted to float for parameters with FLOAT type.
     */
    private Object convertToType(Object value) {
        if (type == Type.INT) {
            if (value instanceof Integer) {
                return value;
            } else if (value instanceof Float) {
                return ((Float) value).intValue();
            } else if (value instanceof Double) {
                return ((Double) value).intValue();
            } else {
                throw new IllegalArgumentException("Value " + value + " cannot be converted to int.");
            }
        } else if (type == Type.FLOAT) {
            if (value instanceof Float) {
                return value;
            } else if (value instanceof Integer) {
                return ((Integer) value).floatValue();
            } else if (value instanceof Double) {
                return ((Double) value).floatValue();
            } else {
                throw new IllegalArgumentException("Value " + value + " cannot be converted to float.");
            }
        } else if (type == Type.STRING) {
            return value.toString();
        } else if (type == Type.COLOR) {
            if (value instanceof Color) {
                return value;
            } else if (value instanceof Integer) {
                float v = ((Integer) value) / 255f;
                return new Color(v, v, v);
            } else if (value instanceof Float) {
                float v = ((Float) value);
                return new Color(v, v, v);
            } else if (value instanceof Double) {
                double v = ((Double) value);
                return new Color(v, v, v);
            } else if (value instanceof String) {
                return new Color((String) value);
            } else {
                throw new IllegalArgumentException("Value " + value + " cannot be converted to color.");
            }
        } else {
            return value;
        }
    }

    //// Values ////

    /**
     * Revert the value to its default value.
     * <p/>
     * If this parameter has a prototype, set the value/expression
     * to the value/expression of the prototype.
     */
    public void revertToDefault() {
        Parameter protoParam = getPrototype();
        // Check if we have a prototype parameter and if it is of the same type.
        // Otherwise, use the default value for the Type.
        if (protoParam == null || protoParam.getType() != getType()) {
            // The default is to not have expressions, so they get removed.
            clearExpression();
            setValue(getDefaultValue(type));
        } else if (protoParam.hasExpression()) {
            // If the prototype has an expression, we inherit this expression.
            // TODO: Inheriting the prototype expression can cause problems.
            // It can refer to other parameters that are not in our namespace.
            // Better is to rewrite the expression, which we should also do in clone.

            // 1. Clear out any expression we already have.
            clearExpression();

            //2. Set a default value so accessing the value provides a meaningful default, even before an update.
            // The parameter is dirty, so it needs to be updated anyway.
            // We *could* copy the value for the prototype, but we can't be sure the prototype has
            // been updated, so it's better to set a default value.
            setValue(getDefaultValue(type));

            // 3. Set the expression to the parameter prototype expression.
            setExpression(protoParam.getExpression());
        } else {
            // If the prototype does not have an expression, we shouldn't have on either.
            // Clear it and set the default value to that of the prototype.
            clearExpression();
            setValue(protoParam.getValue());
        }
    }

    /**
     * Get the default value for a Parameter.
     * <p/>
     * The default value is the value of the prototype of the Parameter.
     * If the prototype does not have this parameter or if the prototype
     * parameter is of a different type, return the default value for the Type.
     *
     * @return the default value for this type.
     * @see #getDefaultValue(Type)
     */
    public Object getDefaultValue() {
        Parameter protoParam = getPrototype();
        // If this parameter has no prototype or if the prototype is of a different type,
        // return the default value for this type.
        if (protoParam == null || protoParam.getType() != getType()) return getDefaultValue(type);
        return protoParam.getValue();
    }

    /**
     * Get the default value for the given Type.
     * <p/>
     * This returns a sane default.
     *
     * @param type the type to lookup
     * @return the default value for this type.
     */
    public static Object getDefaultValue(Type type) {
        if (type == Type.INT) {
            return 0;
        } else if (type == Type.FLOAT) {
            return 0F;
        } else if (type == Type.STRING) {
            return "";
        } else if (type == Type.COLOR) {
            return new Color();
        } else if (type == Type.CODE) {
            return emptyCode;
        } else {
            return null;
        }
    }

    /**
     * Try to coerce the given string into a correct value for this parameter.
     * <p/>
     * This does not change the value for this parameter. Use the returned value with Node.setValue().
     * <p/>
     * This method throws a NumberFormatException if type value could not be parsed.
     *
     * @param value the value to parse
     * @return the value converted to the correct type.
     * @throws IllegalArgumentException when the given value could not be parsed.
     * @see Node#setValue(String, Object) after parsing, use this method to set the value on the parameter.
     */
    public Object parseValue(String value) throws IllegalArgumentException {
        return parseValue(value, type);
    }

    /**
     * Try to coerce the string into a correct value of the given type.
     * <p/>
     * This does not change the value for this parameter. Use the returned value with Node.setValue().
     * <p/>
     * This method throws a IllegalArgumentException if type value could not be parsed.
     * <p/>
     * Code objects cannot be parsed.
     *
     * @param value the value to parse
     * @param type  the type to convert to
     * @return the value converted to the correct type.
     * @throws IllegalArgumentException when the given value could not be parsed.
     * @see Node#setValue(String, Object) after parsing, use this method to set the value on the parameter.
     */
    public static Object parseValue(String value, Type type) throws IllegalArgumentException {
        if (type == Type.INT) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(e);
            }
        } else if (type == Type.FLOAT) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(e);
            }
        } else if (type == Type.COLOR) {
            return Color.parseColor(value);
        } else if (type == Type.CODE) {
            throw new IllegalArgumentException("Cannot parse code objects.");
        } else {
            return value;
        }
    }

    public boolean prototypeEquals(Parameter o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parameter parameter = (Parameter) o;

        if (boundingMethod != parameter.boundingMethod) return false;
        if (displayLevel != parameter.displayLevel) return false;
        if (expression != null ? !expression.equals(parameter.expression) : parameter.expression != null) return false;
        if (helpText != null ? !helpText.equals(parameter.helpText) : parameter.helpText != null) return false;
        if (!label.equals(parameter.label)) return false;
        if (enableExpression != null ? !enableExpression.equals(parameter.enableExpression) : parameter.enableExpression != null)
            return false;
        if (maximumValue != null ? !maximumValue.equals(parameter.maximumValue) : parameter.maximumValue != null)
            return false;
        if (!menuItems.equals(parameter.menuItems)) return false;
        if (minimumValue != null ? !minimumValue.equals(parameter.minimumValue) : parameter.minimumValue != null)
            return false;
        if (!name.equals(parameter.name)) return false;
        if (type != parameter.type) return false;
        if (expression == null)
            if (value != null ? !value.equals(parameter.value) : parameter.value != null) return false;
        if (widget != parameter.widget) return false;

        return true;
    }

    @Override
    public String toString() {
        return "<Parameter " + getNode().getName() + "." + getName() + " (" + getType().toString().toLowerCase() + ")>";
    }

    /**
     * Copy this parameter and changes expressions.
     * <p/>
     * The difference between copyWithUpstream and clone is that copyWithUpstream also copies the value,
     * whereas clone inherits the value from the prototype parameter.
     *
     * @param newNode the new node that will act as the parent to this parameter.
     * @return the new copy of this parameter.
     */
    public Parameter copyWithUpstream(Node newNode) {
        Parameter p = new Parameter(newNode, getName(), getType());
        if (hasExpression()) {
            p.setExpression(getExpression());
        } else {
            p.setValue(getValue());
        }
        copyAttributes(p);
        return p;
    }

    /**
     * Clone the parameter so that it can be added to the given node.
     * <p/>
     * The value/expression of the new parameter will match the value/parameter
     * of its prototype, if that exists, otherwise the value will be set to the
     * default value for the Type. If the parameter has an expression, the value will be set to the
     * default value for the Type as well. You need to update the parameter to evaluate the expression
     * and get a correct value.
     * <p/>
     * Do not use this method directly. This method is only used by Node to create a new instance
     * based on a prototype.
     *
     * @param n the new node this parameter should be under.
     * @return a new Parameter.
     * @see Node#newInstance(NodeLibrary, String, Class)
     */
    public Parameter clone(Node n) {
        // This will call revertToDefault, which will set the value/expression to that of the prototype.
        Parameter p = new Parameter(n, getName(), getType());
        copyAttributes(p);
        return p;
    }

    /**
     * Copy all metadata attributes of this parameter into the given parameter.
     *
     * @param p the parameter to copy onto.
     */
    private void copyAttributes(Parameter p) {
        p.setLabel(getLabel());
        p.setHelpText(getHelpText());
        p.setWidget(getWidget());
        p.setEnableExpression(getEnableExpression());
        p.setBoundingMethod(getBoundingMethod());
        p.setMinimumValue(getMinimumValue());
        p.setMaximumValue(getMaximumValue());
        p.setDisplayLevel(getDisplayLevel());
        for (MenuItem item : getMenuItems()) {
            p.addMenuItem(item.getKey(), item.getLabel());
        }
    }

    private static class EmptyCode implements NodeCode {
        public Object cook(Node node, ProcessingContext context) {
            return null;
        }

        public String getSource() {
            return "";
        }

        public String getType() {
            return "";
        }
    }
}
