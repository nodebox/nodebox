package net.nodebox.node;

import java.util.HashMap;

/**
 * <p>A ParameterType holds the meta-information for a Node parameter.</p>
 * <p/>
 * <p>A ParameterType holds information for a to-be-instantiated parameter,
 * such as its name, type and default value. In object-speak, this is the class:
 * the actual instance of this parameter (its value) is not stored here,
 * but in a Parameter object, contained in the Node instance.</p>
 */
public class ParameterType {

    static class TypeTemplate {
        public CoreType coreType;
        public int channels;

        TypeTemplate(CoreType coreType, int channels) {
            this.coreType = coreType;
            this.channels = channels;
        }
    }


    public enum CoreType {
        INT, FLOAT, STRING, DATA
    }

    public static final HashMap<CoreType, Class> CORE_TYPE_MAPPING;
    public static final HashMap<CoreType, Object> CORE_TYPE_DEFAULTS;


    public static final String TYPE_ANGLE = "angle";
    public static final String TYPE_COLOR = "color";
    public static final String TYPE_CUSTOM = "custom";
    public static final String TYPE_FILE = "file";
    public static final String TYPE_FLOAT = "float";
    public static final String TYPE_FONT = "font";
    public static final String TYPE_GRADIENT = "gradient";
    public static final String TYPE_GROUP = "group";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_INT = "int";
    public static final String TYPE_MENU = "menu";
    public static final String TYPE_POINT = "point";
    public static final String TYPE_SEED = "seed";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_TOGGLE = "toggle";

    public static final HashMap<String, TypeTemplate> TYPE_REGISTRY;

    public enum Direction {
        IN, OUT
    }

    public enum Bounding {
        NONE, SOFT, HARD
    }

    public enum DisplayLevel {
        HIDDEN, DETAIL, HUD
    }

    static {
        CORE_TYPE_MAPPING = new HashMap<CoreType, Class>();
        CORE_TYPE_MAPPING.put(CoreType.INT, int.class);
        CORE_TYPE_MAPPING.put(CoreType.FLOAT, float.class);
        CORE_TYPE_MAPPING.put(CoreType.STRING, String.class);
        CORE_TYPE_MAPPING.put(CoreType.DATA, Object.class);

        CORE_TYPE_DEFAULTS = new HashMap<CoreType, Object>();
        CORE_TYPE_DEFAULTS.put(CoreType.INT, 0);
        CORE_TYPE_DEFAULTS.put(CoreType.FLOAT, 0);
        CORE_TYPE_DEFAULTS.put(CoreType.STRING, "");
        CORE_TYPE_DEFAULTS.put(CoreType.DATA, null);

        TYPE_REGISTRY = new HashMap<String, TypeTemplate>();
        TYPE_REGISTRY.put(TYPE_ANGLE, new TypeTemplate(CoreType.FLOAT, 1));
        TYPE_REGISTRY.put(TYPE_COLOR, new TypeTemplate(CoreType.FLOAT, 4));
        TYPE_REGISTRY.put(TYPE_CUSTOM, new TypeTemplate(CoreType.DATA, 1));
        TYPE_REGISTRY.put(TYPE_FILE, new TypeTemplate(CoreType.STRING, 1));
        TYPE_REGISTRY.put(TYPE_FLOAT, new TypeTemplate(CoreType.FLOAT, 1));
        TYPE_REGISTRY.put(TYPE_FONT, new TypeTemplate(CoreType.STRING, 1));
        TYPE_REGISTRY.put(TYPE_GRADIENT, new TypeTemplate(CoreType.STRING, 1));
        TYPE_REGISTRY.put(TYPE_GROUP, new TypeTemplate(CoreType.INT, 1));
        TYPE_REGISTRY.put(TYPE_INT, new TypeTemplate(CoreType.STRING, 1));
        TYPE_REGISTRY.put(TYPE_MENU, new TypeTemplate(CoreType.INT, 1));
        TYPE_REGISTRY.put(TYPE_POINT, new TypeTemplate(CoreType.FLOAT, 2));
        TYPE_REGISTRY.put(TYPE_SEED, new TypeTemplate(CoreType.INT, 1));
        TYPE_REGISTRY.put(TYPE_STRING, new TypeTemplate(CoreType.STRING, 1));
        TYPE_REGISTRY.put(TYPE_TEXT, new TypeTemplate(CoreType.STRING, 1));
        TYPE_REGISTRY.put(TYPE_TOGGLE, new TypeTemplate(CoreType.INT, 1));
    }

    private NodeType nodeType;
    private String name;
    private String label;
    private String type;
    private CoreType coreType;
    private int channels;
    private Direction direction;
    private Object[] defaultValue;

    //// Initialization ////

    public ParameterType(NodeType nodeType, String name, String type) {
        this(nodeType, name, type, Direction.IN);
    }

    public ParameterType(NodeType nodeType, String name, String type, Direction direction) {
        assert (TYPE_REGISTRY.containsKey(type));
        this.nodeType = nodeType;
        this.name = name;
        this.label = name;
        this.type = type;
        changeType(type); // this sets the core type and number of channels
        this.direction = direction;
        Object valueSingle = CORE_TYPE_DEFAULTS.get(this.coreType);
        this.defaultValue = new Object[this.channels];
        for (int i = 0; i < this.channels; i++) {
            this.defaultValue[i] = valueSingle;
        }
    }


    //// Attribute access ////

    public NodeType getNodeType() {
        return nodeType;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    private void changeType(String type) {
        //TODO: This is actually a setter for type, but it is not made public yet.
        // It has repercussions for all instantiated nodes which we don't handle yet.
        if (this.type.equals(type)) return;
        this.type = type;
        TypeTemplate t = TYPE_REGISTRY.get(type);
        this.coreType = t.coreType;
        this.channels = t.channels;
    }

    public Direction getDirection() {
        return direction;
    }

    public static HashMap<CoreType, Class> getCoreTypeMapping() {
        return CORE_TYPE_MAPPING;
    }

    public CoreType getCoreType() {
        return coreType;
    }

    public int getChannels() {
        return channels;
    }

    public Object[] getDefaultValue() {
        return defaultValue;
    }

    public boolean isInputParameter() {
        return direction == Direction.IN;
    }

    public boolean isOutputParameter() {
        return direction == Direction.OUT;
    }

    /**
     * Returns whether this type is compatible with the given Node.
     *
     *   All this does is compare my type with the parameter type of the output parameter,
     *   of the given Node.
     *
     *   The check is very strict. The two types have to match exactly. In the future, we might
     *   loosen this constraint to also accept types with the same core type and number of channels.
     * @param node the node to check
     * @return true if this type is compatible.
     */
    public boolean isCompatible(Node node) {
        return type.equals(node.getNodeType().getOutputParameterType().getType());
    }

    public boolean isCompatible(NodeType nodeType) {
        return type.equals(nodeType.getOutputParameterType().getType());
    }

    public boolean isCompatible(Parameter parameter) {
        return type.equals(parameter.getParameterType().getType());
    }

    public boolean isCompatible(ParameterType parameterType) {
        return type.equals(parameterType.getType());
    }

    //// Validation ////

    public void validate(Object value) {
        // TODO: test for list-likeness?
    }

    //// Parsing ////

    public Object parseSingleValue(String value) {
        switch (coreType) {
            case INT:
                return Integer.parseInt(value);
             case FLOAT:
                 return Float.parseFloat(value);
            case STRING:
                return value;
            default:
                return value;
        }
    }

    //// Instance creation ////

    public Parameter createParameter(Node node) {
        if (isInputParameter()) {
            return new Parameter(this, node);
        } else {
            return new OutputParameter(this, node);
        }
    }


}
