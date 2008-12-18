package net.nodebox.node;

import net.nodebox.graphics.*;
import net.nodebox.util.StringUtils;

import javax.swing.event.EventListenerList;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A ParameterType holds the meta-information for a Node parameter.
 * <p/>
 * A ParameterType holds information for a to-be-instantiated parameter,
 * such as its name, type and default value. In object-speak, this is the class:
 * the actual instance of this parameter (its value) is not stored here,
 * but in a Parameter object, contained in the Node instance.
 */
public class ParameterType extends Observable {

    public enum Type {
        ANGLE, COLOR, FILE, FLOAT, FONT, GRADIENT, IMAGE, INT, MENU, SEED, STRING, TEXT, TOGGLE, NODEREF,
        GROB_CANVAS, GROB_VECTOR, GROB_IMAGE
    }

    public enum CoreType {
        INT, FLOAT, STRING, COLOR, GROB_CANVAS, GROB_SHAPE, GROB_IMAGE
    }

    public enum Direction {
        IN, OUT
    }

    public enum BoundingMethod {
        NONE, SOFT, HARD
    }

    public enum DisplayLevel {
        HIDDEN, DETAIL, HUD
    }

    public static final HashMap<CoreType, Class> CORE_TYPE_MAPPING;
    public static final HashMap<CoreType, Object> CORE_TYPE_DEFAULTS;
    public static final HashMap<Type, CoreType> TYPE_REGISTRY;

    public class MenuEntry {
        private String key;
        private String label;

        public MenuEntry(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }
    }

    static {
        CORE_TYPE_MAPPING = new HashMap<CoreType, Class>();
        CORE_TYPE_MAPPING.put(CoreType.INT, Integer.class);
        CORE_TYPE_MAPPING.put(CoreType.FLOAT, Double.class);
        CORE_TYPE_MAPPING.put(CoreType.STRING, String.class);
        CORE_TYPE_MAPPING.put(CoreType.COLOR, Color.class);
        CORE_TYPE_MAPPING.put(CoreType.GROB_CANVAS, Canvas.class);
        CORE_TYPE_MAPPING.put(CoreType.GROB_SHAPE, Group.class);
        CORE_TYPE_MAPPING.put(CoreType.GROB_IMAGE, Image.class);

        CORE_TYPE_DEFAULTS = new HashMap<CoreType, Object>();
        CORE_TYPE_DEFAULTS.put(CoreType.INT, 0);
        CORE_TYPE_DEFAULTS.put(CoreType.FLOAT, 0.0);
        CORE_TYPE_DEFAULTS.put(CoreType.STRING, "");
        CORE_TYPE_DEFAULTS.put(CoreType.COLOR, new Color());
        CORE_TYPE_DEFAULTS.put(CoreType.GROB_CANVAS, new Canvas());
        CORE_TYPE_DEFAULTS.put(CoreType.GROB_SHAPE, new Group());
        CORE_TYPE_DEFAULTS.put(CoreType.GROB_IMAGE, new Image());

        TYPE_REGISTRY = new HashMap<Type, CoreType>();
        TYPE_REGISTRY.put(Type.ANGLE, CoreType.FLOAT);
        TYPE_REGISTRY.put(Type.COLOR, CoreType.COLOR);
        TYPE_REGISTRY.put(Type.FILE, CoreType.STRING);
        TYPE_REGISTRY.put(Type.FLOAT, CoreType.FLOAT);
        TYPE_REGISTRY.put(Type.FONT, CoreType.STRING);
        TYPE_REGISTRY.put(Type.GRADIENT, CoreType.STRING);
        TYPE_REGISTRY.put(Type.IMAGE, CoreType.STRING);
        TYPE_REGISTRY.put(Type.INT, CoreType.INT);
        TYPE_REGISTRY.put(Type.MENU, CoreType.STRING);
        TYPE_REGISTRY.put(Type.SEED, CoreType.INT);
        TYPE_REGISTRY.put(Type.STRING, CoreType.STRING);
        TYPE_REGISTRY.put(Type.TEXT, CoreType.STRING);
        TYPE_REGISTRY.put(Type.TOGGLE, CoreType.INT);
        TYPE_REGISTRY.put(Type.NODEREF, CoreType.STRING);
        TYPE_REGISTRY.put(Type.GROB_CANVAS, CoreType.GROB_CANVAS);
        TYPE_REGISTRY.put(Type.GROB_VECTOR, CoreType.GROB_SHAPE);
        TYPE_REGISTRY.put(Type.GROB_IMAGE, CoreType.GROB_IMAGE);
    }

    public static final Pattern RESERVED_WORD_PATTERN = Pattern.compile("^(network|node|name)$");

    private NodeType nodeType;
    private String name;
    private String label;
    private String description;
    private Type type;
    private CoreType coreType;
    private Direction direction;
    private Object defaultValue;
    private boolean nullAllowed = false;
    private BoundingMethod boundingMethod;
    private Double minimumValue;
    private Double maximumValue;
    private DisplayLevel displayLevel;
    private ArrayList<MenuEntry> menuItems = new ArrayList<MenuEntry>();
    private EventListenerList listeners = new EventListenerList();

    public ParameterType(NodeType nodeType, String name, Type type) {
        this(nodeType, name, type, Direction.IN);
    }

    public ParameterType(NodeType nodeType, String name, Type type, Direction direction) {
        this.nodeType = nodeType;
        validateName(name);
        this.name = name;
        this.label = StringUtils.humanizeName(name);
        setType(type);  // this sets the core type, default values, and value.
        this.direction = direction;
    }

    //// Attribute access ////

    public NodeType getNodeType() {
        return nodeType;
    }

    //// Naming ////

    public void validateName(String name) {
        // Check if another parameter has the same name.
        if (nodeType.hasParameterType(name) && nodeType.getParameterType(name) != this) {
            throw new InvalidNameException(this, name, "Node type " + nodeType.getIdentifier() + " already has a parameter type named '" + name + "'.");

        }
        NodeType.validateName(name);
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        // TODO: fireLabelChanged();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        // TODO: fireDescriptionChanged();
    }

    //// Type ////

    public Type getType() {
        return type;
    }

    public boolean isPrimitive() {
        return (coreType == CoreType.INT || coreType == CoreType.FLOAT || coreType == CoreType.STRING || coreType == CoreType.COLOR);
    }

    public void setType(Type type) {
        if (this.type == type) return;
        this.type = type;
        assert (TYPE_REGISTRY.containsKey(type));
        this.coreType = TYPE_REGISTRY.get(type);
        this.defaultValue = CORE_TYPE_DEFAULTS.get(this.coreType);
        fireTypeChanged();
    }


    public CoreType getCoreType() {
        return coreType;
    }

    public Class getTypeClass() {
        return CORE_TYPE_MAPPING.get(coreType);
    }

    //// Direction ////

    public boolean isInputParameter() {
        return direction == Direction.IN;
    }

    public boolean isOutputParameter() {
        return direction == Direction.OUT;
    }

    public Direction getDirection() {
        return direction;
    }

    //// Default value ////

    /**
     * Returns a copy of the getDefaultValue.
     *
     * @return the default value for this parameter type
     */
    public Object getDefaultValue() {
        if (isPrimitive() && getCoreType() != CoreType.COLOR) {
            return defaultValue;
        } else if (getCoreType() == CoreType.COLOR) {
            return ((Color) defaultValue).clone();
        } else { // One of the grob types
            return ((Grob) defaultValue).clone();
        }
    }

    public void setDefaultValue(Object value) {
        validate(value);
        defaultValue = value;
    }

    //// Null ////

    public boolean isNullAllowed() {
        return nullAllowed;
    }

    public void setNullAllowed(boolean value) {
        nullAllowed = value;
        fireNullAllowedChanged();
    }

    //// Boundaries ////

    public BoundingMethod getBoundingMethod() {
        return boundingMethod;
    }

    public void setBoundingMethod(BoundingMethod boundingMethod) {
        this.boundingMethod = boundingMethod;
        fireBoundingChanged();
    }

    public Double getMinimumValue() {
        return minimumValue;
    }

    public void setMinimumValue(Double minimumValue) {
        if (this.minimumValue != null && this.minimumValue.equals(minimumValue))
            return;
        if (minimumValue != null && maximumValue != null && minimumValue > maximumValue)
            return;
        // Create copy of minimumValue
        this.minimumValue = (double) minimumValue;
        fireBoundingChanged();
    }

    public void setMaximumValue(Double maximumValue) {
        if (this.maximumValue != null && this.maximumValue.equals(maximumValue))
            return;
        if (minimumValue != null && maximumValue != null && maximumValue < minimumValue)
            return;
        // Create copy of maximumValue
        this.maximumValue = (double) maximumValue;
        fireBoundingChanged();
    }

    public Double getMaximumValue() {
        return maximumValue;
    }

    public boolean valueCorrectForBounds(double value) {
        if (boundingMethod != BoundingMethod.HARD) {
            return true;
        }
        return value >= minimumValue && value <= maximumValue;
    }

    //// Display level ////

    public DisplayLevel getDisplayLevel() {
        return displayLevel;
    }

    public void setDisplayLevel(DisplayLevel displayLevel) {
        this.displayLevel = displayLevel;
        fireDisplayLevelChanged();
    }

    //// Menu items ////

    public List<MenuEntry> getMenuItems() {
        return menuItems;
    }

    public void addMenuItem(String key, String label) {
        menuItems.add(new MenuEntry(key, label));
        // TODO: fireMenuChanged();
    }

    //// Validation ////

    /**
     * Checks if the given value would fit this parameter.
     * <p/>
     * Raises a ValueError if the value does not match.
     *
     * @param value the value to validate.
     */
    public void validate(Object value) {
        if (value == null && !isNullAllowed()) {
            throw new ValueError("Value for parameter " + getName() + " cannot be null.");
        }
        // Check if the type matches
        Class requiredType = CORE_TYPE_MAPPING.get(coreType);
        if (!value.getClass().isAssignableFrom(requiredType)) {
            throw new ValueError("Value is not of the required type (" + requiredType.getSimpleName() + ")");
        }
        // If hard bounds are set, check if the value falls within the bounds.

        if (getBoundingMethod() == BoundingMethod.HARD) {
            double doubleValue = (Double) value;
            // TODO: Check if bounding is implemented correctly.
//            if (value instanceof Integer) {
//                doubleValue = (Double) value;
//            } else if (value instanceof Double) {
//                doubleValue = (Double) value;
//            }
            if (minimumValue != null && doubleValue < minimumValue) {
                throw new ValueError("Parameter " + getName() + ": value " + value + " is too small. (minimum=" + minimumValue + ")");
            }
            if (maximumValue != null && doubleValue > maximumValue) {
                throw new ValueError("Parameter " + getName() + ": value " + value + " is too big. (maximum=" + maximumValue + ")");
            }
        }
    }

    //// Parsing ////

    public Object parseValue(String value) throws NumberFormatException {
        switch (coreType) {
            case INT:
                return Integer.parseInt(value);
            case FLOAT:
                return Double.parseDouble(value);
            case STRING:
                return value;
            case COLOR:
                return new Color(value);
            default:
                return value;
        }
    }

    //// Instance creation ////

    public Parameter createParameter(Node node) {
        if (getDirection() == Direction.IN) {
            return new Parameter(this, node);
        } else {
            return new OutputParameter(this, node);
        }
    }

    //// Event handling ////

    public void addParameterTypeListener(ParameterTypeListener l) {
        listeners.add(ParameterTypeListener.class, l);
    }

    public void removeParameterTypeListener(ParameterTypeListener l) {
        listeners.remove(ParameterTypeListener.class, l);
    }

    private void fireTypeChanged() {
        assert (listeners != null);
        for (EventListener l : listeners.getListeners(ParameterTypeListener.class))
            ((ParameterTypeListener) l).typeChanged(this);
    }

    private void fireBoundingChanged() {
        assert (listeners != null);
        for (EventListener l : listeners.getListeners(ParameterTypeListener.class))
            ((ParameterTypeListener) l).boundingChanged(this);
    }

    private void fireDisplayLevelChanged() {
        assert (listeners != null);
        for (EventListener l : listeners.getListeners(ParameterTypeListener.class))
            ((ParameterTypeListener) l).displayLevelChanged(this);
    }

    private void fireNullAllowedChanged() {
        assert (listeners != null);
        for (EventListener l : listeners.getListeners(ParameterTypeListener.class))
            ((ParameterTypeListener) l).nullAllowedChanged(this);
    }

    //// Cloning ////

    /**
     * Creates a copy of this parameter type. The cloned copy will be attached to the given nodeType,
     * instead of the current node type.
     *
     * @param nodeType the new nodeType this parameter type attaches to.
     * @return a cloned copy of this node type.
     */
    public ParameterType clone(NodeType nodeType) {
        ParameterType newType = new ParameterType(nodeType, getName(), getType(), getDirection());
        newType.label = getLabel();
        newType.description = getDescription();
        // TODO: Does this actually make copies of the values? check!
        newType.defaultValue = getDefaultValue();
        newType.nullAllowed = isNullAllowed();
        newType.boundingMethod = getBoundingMethod();
        // TODO: Does this actually make copies of the values? check!
        newType.minimumValue = getMinimumValue();
        newType.maximumValue = getMaximumValue();
        newType.displayLevel = getDisplayLevel();
        newType.menuItems = new ArrayList<MenuEntry>(menuItems);
        return newType;
    }

}
