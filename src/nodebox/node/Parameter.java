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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
        ANGLE, COLOR, FILE, FLOAT, FONT, GRADIENT, IMAGE, INT, MENU, SEED, STRING, TEXT, TOGGLE, NODEREF, CODE
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
        node.fireNodeAttributeChanged(NodeAttributeListener.Attribute.PARAMETER);
    }

    void _setName(String name) {
        this.name = name;
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
        node.fireNodeAttributeChanged(NodeAttributeListener.Attribute.PARAMETER);
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
        node.fireNodeAttributeChanged(NodeAttributeListener.Attribute.PARAMETER);
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
     * @param newType
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
        this.widget = widget;
        fireAttributeChanged();
    }

    public static Widget getDefaultWidget(Type type) {
        return WIDGET_MAPPING.get(type);
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
        node.fireParameterAttributeChanged(this);
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
            return String.format("Color(%.2f, %.2f, %.2f, %.2f)", v.getRed(), v.getGreen(), v.getBlue(), v.getAlpha());
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

    /**
     * Return a reference to the original value stored in this parameter.
     * <p/>
     * This method exists for performance reasons; however, make sure not to modify the reference.
     *
     * @return a reference to the original value stored in the parameter.
     */
    public Object getValueReference() {
        return value;
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
        if (this.value != null && this.value.equals(value)) return;
        // As a special exception, integer values can be cast up to floating-point values,
        // and double values can be cast down (losing precision).
        if (value instanceof Integer && type == Type.FLOAT) {
            this.value = (float) ((Integer) value);
        } else if (value instanceof Double && type == Type.FLOAT) {
            this.value = (float) ((Double) value).doubleValue();
        } else {
            this.value = value;
        }
        markDirty();
    }

    /**
     * Check if this parameter is dirty and needs to be updated.
     *
     * @return true if the parameter is dirty.
     */
    public boolean isDirty() {
        return dirty;
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

    public String getExpression() {
        return hasExpression() ? expression.getExpression() : "";
    }

    public void clearExpression() {
        this.expression = null;
        hasStampExpression = false;
        removeDependencies();
        markDirty();
    }

    public void setExpression(String expression) throws ExpressionError {
        if (hasExpression() && getExpression().equals(expression)) {
            return;
        }
        if (expression == null || expression.trim().length() == 0) {
            clearExpression();
        } else {
            this.expression = new Expression(this, expression);
            // Reset the stamp flag. It will be set by markStampExpression(), which will be called
            // from the expression helper while evaluating the expression.
            hasStampExpression = false;
            // Evaluate the expression to see if it returns any errors.
            this.expression.evaluate();
            // Setting an expession automatically enables it and marks the parameter as dirty.
            markDirty();
            try {
                updateDependencies();
            } catch (IllegalArgumentException e) {
                // Whilst updating, we might catch a Connection error meaning you are connecting
                // e.g. the parameter to itself. If that happens, we clear out the expression and all of its
                // dependencies.
                removeDependencies();
                this.expression = null;
                throw new ExpressionError("This expression causes a cyclic dependency.", e);
            }
        }
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
     * Parameter depencies are created by setting expressions that refer to other parameters. Once these parameters
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
//
//
//        for (WeakReference<Parameter> ref : dependencies) {
//            Parameter p = ref.get();
//            if (p != null)
//                p.removeDependent(this);
//        }
//        dependencies.clear();
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
            p.dependencyChangedEvent(this);
        }
        getLibrary().removeParameterDependents(this);
//        for (WeakReference<Parameter> ref : dependents) {
//            Parameter p = ref.get();
//            if (p != null)
//                p.removeDependency(this);
//        }
//        dependents.clear();
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
//        Set<Parameter> set = new HashSet<Parameter>(dependents.size());
//        for (WeakReference<Parameter> ref : dependents) {
//            Parameter p = ref.get();
//            if (p != null)
//                set.add(p);
//        }
//        return set;
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
//        Set<Parameter> set = new HashSet<Parameter>(dependencies.size());
//        for (WeakReference<Parameter> ref : dependencies) {
//            Parameter p = ref.get();
//            if (p != null)
//                set.add(p);
//        }
//        return set;
    }

    /**
     * Called whenever the value of this parameter changes. This method informs the dependent parameters that my value
     * has changed.
     */
    protected void fireValueChanged() {
        getNode().fireParameterValueChanged(this);
        getNode().markDirty();
        for (Parameter p : getDependents()) {
            p.dependencyChangedEvent(this);
        }
//        for (WeakReference<Parameter> ref : dependents) {
//            Parameter p = ref.get();
//            if (p != null)
//                p.dependencyChangedEvent(p);
//        }
//        for (ParameterValueListener l : listeners) {
//            l.valueChanged(this, value);
//        }
    }

    private void dependencyChangedEvent(Parameter p) {
        markDirty();
        //getNode().markDirty();
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
            validate(expressionValue);
            value = convertToType(expressionValue);
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
        if (type != Type.FLOAT) return value;
        if (value instanceof Float) return value;
        if (value instanceof Integer) return ((Integer) value).floatValue();
        if (value instanceof Double) return ((Double) value).floatValue();
        throw new IllegalArgumentException("Value " + value + " cannot be converted to float.");
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
            // If the prototype has an expression, we need to have an expression too.
            // TODO: Inheriting the prototype expression is simple, but likely wrong.
            // It can refer to other parameters that are not in our namespace.
            // Better is to rewrite the expression, which we should also do in clone.
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

    //// Persistence ////

    /**
     * Converts the data structure to xml. The xml String is appended to the given StringBuffer.
     *
     * @param xml    the StringBuffer to use when appending.
     * @param spaces the indentation.
     * @see Node#toXml
     */
    public void toXml(StringBuffer xml, String spaces) {
        // We only write out the attributes that have changed with regards to the prototype.
        Parameter protoParam = getPrototype();
        // If the parameter and its prototype are completely equal, don't write anything.
        if (prototypeEquals(protoParam)) return;
        // The parameters are not equal, so we can start writing the name.
        xml.append(spaces).append("<param name=\"").append(getName()).append("\"");
        // Write parameter type
        if (protoParam == null || !getType().equals(protoParam.getType()))
            xml.append(" ").append(NDBXHandler.PARAMETER_TYPE).append("=\"").append(getType().toString().toLowerCase()).append("\"");
        // Write parameter attributes
        attributeToXml(xml, "widget", NDBXHandler.PARAMETER_WIDGET, protoParam, WIDGET_MAPPING.get(type));
        attributeToXml(xml, "label", NDBXHandler.PARAMETER_LABEL, protoParam, StringUtils.humanizeName(name));
        attributeToXml(xml, "helpText", NDBXHandler.PARAMETER_HELP_TEXT, protoParam, null);
        attributeToXml(xml, "displayLevel", NDBXHandler.PARAMETER_DISPLAY_LEVEL, protoParam, DisplayLevel.HUD);
        attributeToXml(xml, "boundingMethod", NDBXHandler.PARAMETER_BOUNDING_METHOD, protoParam, BoundingMethod.NONE);
        attributeToXml(xml, "minimumValue", NDBXHandler.PARAMETER_MINIMUM_VALUE, protoParam, null);
        attributeToXml(xml, "maximumValue", NDBXHandler.PARAMETER_MAXIMUM_VALUE, protoParam, null);
        xml.append(">\n");
        // Write parameter value / expression
        if (hasExpression()) {
            xml.append(spaces).append("  <expression>").append(getExpression()).append("</expression>\n");
        } else {
            if (type == Type.INT) {
                xml.append(spaces).append("  <value>").append(asInt()).append("</value>\n");
            } else if (type == Type.FLOAT) {
                xml.append(spaces).append("  <value>").append(asFloat()).append("</value>\n");
            } else if (type == Type.STRING) {
                xml.append(spaces).append("  <value>").append(asString()).append("</value>\n");
            } else if (type == Type.COLOR) {
                xml.append(spaces).append("  <value>").append(asColor().toString()).append("</value>\n");
            } else if (type == Type.CODE) {
                xml.append(spaces).append("  <value type=\"").append(asCode().getType()).append("\"><![CDATA[").append(asCode().getSource()).append("]]></value>\n");
            } else {
                throw new AssertionError("Unknown value class " + type);
            }
        }
        // Write menu items
        if (menuItems.size() > 0) {
            List<Parameter.MenuItem> protoItems = protoParam == null ? null : protoParam.getMenuItems();
            if (!menuItems.equals(protoItems)) {
                for (MenuItem item : menuItems) {
                    xml.append(spaces).append("  <menu key=\"").append(item.getKey()).append("\">").append(item.getLabel()).append("</menu>\n");
                }
            }
        }
        xml.append(spaces).append("</param>\n");
    }

    private void attributeToXml(StringBuffer xml, String attrName, String xmlName, Parameter protoParam, Object defaultValue) {
        try {
            String methodName = "get" + attrName.substring(0, 1).toUpperCase() + attrName.substring(1);
            Method m = getClass().getMethod(methodName);
            Object myValue = m.invoke(this);
            if (myValue == null) return;
            Object protoValue = protoParam != null ? m.invoke(protoParam) : null;
            if (!myValue.equals(protoValue) && !myValue.equals(defaultValue)) {
                // Values that are already strings are written as is.
                // Other values, such as enums, are written as lowercase.
                String stringValue = myValue instanceof String ? (String) myValue : myValue.toString().toLowerCase();
                xml.append(" ").append(xmlName).append("=\"").append(stringValue).append("\"");
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while trying to get " + attrName + " for parameter " + this, e);
        }
    }

    private boolean prototypeEquals(Parameter o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parameter parameter = (Parameter) o;

        if (boundingMethod != parameter.boundingMethod) return false;
        if (displayLevel != parameter.displayLevel) return false;
        if (expression != null ? !expression.equals(parameter.expression) : parameter.expression != null) return false;
        if (helpText != null ? !helpText.equals(parameter.helpText) : parameter.helpText != null) return false;
        if (!label.equals(parameter.label)) return false;
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
     * Copy this field and all its upstream connections, recursively.
     * Used with deferreds.
     *
     * @param newNode the new node that will act as the parent to this parameter.
     * @return the new copy of this parameter.
     */
    public Parameter copyWithUpstream(Node newNode) {
        throw new UnsupportedOperationException("Not yet supported.");
        /*
        Constructor parameterConstructor;
        try {
            parameterConstructor = getClass().getConstructor(ParameterType.class, Node.class);
        } catch (NoSuchMethodException e) {
            logger.log(Level.SEVERE, "Class " + getClass() + " has no appropriate constructor.", e);
            return null;
        }
        Parameter newParameter;
        try {
            newParameter = (Parameter) parameterConstructor.newInstance(parameterType, newNode);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Class " + getClass() + " cannot be instantiated.", e);
            return null;
        }

        Connection conn = getParentNode().getExplicitConnection(this);
        if (conn != null) {
            Node newOutputNode = conn.getOutputNode().copyWithUpstream(newNode.getParent());
            newParameter.connect(newOutputNode);
        } else if (hasExpression()) {
            newParameter.setExpression(getExpression());
        } else {
            // TODO: Clone the value properly.
            newParameter.value = value;
        }

        return newParameter;
        */
    }

    /**
     * Clone the parameter so that it can be added to the given node.
     * <p/>
     * The value/expression of the new parameter will match the value/parameter
     * of its prototype, if that exists, otherwise the value will be set to the
     * default value for the Type.
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
        p.setLabel(getLabel());
        p.setHelpText(getHelpText());
        p.setWidget(getWidget());
        p.setBoundingMethod(getBoundingMethod());
        p.setMinimumValue(getMinimumValue());
        p.setMaximumValue(getMaximumValue());
        p.setDisplayLevel(getDisplayLevel());
        for (MenuItem item : getMenuItems()) {
            p.addMenuItem(item.getKey(), item.getLabel());
        }
        return p;
    }

    /**
     * Return a clone of this value.
     * <p/>
     * This method only clones color objects, since other objects are immutable and therefore don't need to be cloned.
     *
     * @param value the original value
     * @return a clone of this value.
     */
    private Object cloneValue(Object value) {
        if (value instanceof Color) {
            return new Color((Color) value);
        } else {
            return value;
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
