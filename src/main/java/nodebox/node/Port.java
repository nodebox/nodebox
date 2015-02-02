package nodebox.node;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import nodebox.graphics.Color;
import nodebox.graphics.Point;
import nodebox.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.*;

public final class Port {

    public static final String TYPE_INT = "int";
    public static final String TYPE_FLOAT = "float";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_POINT = "point";
    public static final String TYPE_COLOR = "color";
    public static final String TYPE_LIST = "list";
    public static final String TYPE_GEOMETRY = "geometry";
    public static final String TYPE_CONTEXT = "context";
    public static final String TYPE_STATE = "state";

    public enum Attribute {NAME, TYPE, LABEL, CHILD_REFERENCE, WIDGET, RANGE, VALUE, DESCRIPTION, MINIMUM_VALUE, MAXIMUM_VALUE, MENU_ITEMS}

    /**
     * The UI control for this port. This defines how the port is represented in the user interface.
     */
    public enum Widget {
        NONE, ANGLE, COLOR, DATA, FILE, FLOAT, FONT, GRADIENT, IMAGE, INT, MENU, SEED, STRING, TEXT, PASSWORD, TOGGLE, POINT
    }

    public enum Direction {
        INPUT, OUTPUT
    }

    public enum Range {
        VALUE, LIST
    }

    public static final Range DEFAULT_RANGE = Range.VALUE;

    public static final ImmutableMap<String, Object> DEFAULT_VALUES;
    public static final ImmutableSet<String> STANDARD_TYPES;
    public static final ImmutableMap<String, ImmutableList<Port.Widget>> WIDGET_MAPPING;

    static {
        ImmutableMap.Builder<String, Object> b = ImmutableMap.builder();
        b.put(TYPE_INT, 0L);
        b.put(TYPE_FLOAT, 0.0);
        b.put(TYPE_BOOLEAN, false);
        b.put(TYPE_STRING, "");
        b.put(TYPE_POINT, Point.ZERO);
        b.put(TYPE_COLOR, Color.BLACK);
        DEFAULT_VALUES = b.build();
        STANDARD_TYPES = ImmutableSet.of(TYPE_INT, TYPE_FLOAT, TYPE_BOOLEAN, TYPE_STRING, TYPE_POINT, TYPE_COLOR);

        ImmutableMap.Builder<String, ImmutableList<Port.Widget>> w = ImmutableMap.builder();
        w.put(TYPE_INT, ImmutableList.of(Widget.INT, Widget.SEED));
        w.put(TYPE_FLOAT, ImmutableList.of(Widget.ANGLE, Widget.FLOAT));
        w.put(TYPE_BOOLEAN, ImmutableList.of(Widget.TOGGLE));
        w.put(TYPE_STRING, ImmutableList.of(Widget.DATA, Widget.FILE, Widget.FONT, Widget.IMAGE, Widget.MENU, Widget.STRING, Widget.TEXT));
        w.put(TYPE_POINT, ImmutableList.of(Widget.POINT));
        w.put(TYPE_COLOR, ImmutableList.of(Widget.COLOR));
        WIDGET_MAPPING = w.build();
    }

    private final String name;
    private final String type;
    private final String label;
    private final String description;
    private final String childReference;
    private final Widget widget;
    private final Range range;
    private final Object value;
    private final Double minimumValue;
    private final Double maximumValue;
    private final ImmutableList<MenuItem> menuItems;

    private final transient int hashCode;

    public static Port intPort(String name, long value) {
        return intPort(name, value, null, null);
    }

    public static Port intPort(String name, long value, Integer minimumValue, Integer maximumValue) {
        checkNotNull(value, "Value cannot be null.");
        return new Port(name, TYPE_INT, value, minimumValue != null ? minimumValue.doubleValue() : null, maximumValue != null ? maximumValue.doubleValue() : null);
    }

    public static Port floatPort(String name, double value) {
        return floatPort(name, value, null, null);
    }

    public static Port floatPort(String name, double value, Double minimumValue, Double maximumValue) {
        checkNotNull(value, "Value cannot be null.");
        return new Port(name, TYPE_FLOAT, value, minimumValue, maximumValue);
    }

    public static Port booleanPort(String name, boolean value) {
        checkNotNull(value, "Value cannot be null.");
        return new Port(name, TYPE_BOOLEAN, value);
    }

    public static Port stringPort(String name, String value) {
        checkNotNull(value, "Value cannot be null.");
        return new Port(name, TYPE_STRING, value);
    }

    public static Port stringPort(String name, String value, Iterable<MenuItem> menuItems) {
        checkNotNull(value, "Value cannot be null.");
        return new Port(name, TYPE_STRING, value, menuItems);
    }

    public static Port pointPort(String name, Point value) {
        checkNotNull(value, "Value cannot be null.");
        return new Port(name, TYPE_POINT, value);
    }

    public static Port colorPort(String name, Color value) {
        checkNotNull(value, "Value cannot be null.");
        return new Port(name, TYPE_COLOR, value);
    }

    public static Port customPort(String name, String type) {
        checkNotNull(type, "Type cannot be null.");
        return new Port(name, type, null);
    }

    public static Port publishedPort(Node childNode, Port childPort, String publishedName) {
        checkNotNull(childNode);
        checkNotNull(childPort);
        String childReference = buildChildReference(childNode, childPort);
        return new Port(publishedName, childPort.getType(), "", childReference, childPort.getWidget(), childPort.getRange(), childPort.getValue(), childPort.getDescription(), childPort.getMinimumValue(), childPort.getMaximumValue(), childPort.getMenuItems());
    }

    /**
     * Parse the type and create the appropriate Port. Use the default value appropriate for the port type.
     *
     * @param name The port name.
     * @param type The port type.
     * @return A new Port.
     */
    public static Port portForType(String name, String type) {
        checkNotNull(type, "Type cannot be null.");
        // If the type is not found in the default values, get() returns null, which is what we need for custom types.
        return new Port(name, type, "", null, defaultWidgetForType(type), DEFAULT_RANGE, DEFAULT_VALUES.get(type), "", null, null, ImmutableList.<MenuItem>of());
    }

    /**
     * Create a new Port with the given value as a string parsed to the correct format.
     *
     * @param name        The port name.
     * @param type        The port type.
     * @param stringValue The port value as a string, e.g. "32.5".
     * @return A new Port.
     */

    public static Port parsedPort(String name, String type, String stringValue) {
        return parsedPort(name, type, "", "", DEFAULT_RANGE.toString().toLowerCase(Locale.US), stringValue, "", null, null, ImmutableList.<MenuItem>of());
    }

    /**
     * Create a new Port with the given value as a string parsed to the correct format.
     *
     * @param name        The port name.
     * @param type        The port type.
     * @param valueString The port value as a string, e.g. "32.5".
     * @param minString   The minimum value as a string.
     * @param maxString   The maximum value as a string.
     * @param menuItems   The list of menu items.
     * @return A new Port.
     */
    public static Port parsedPort(String name, String type, String label, String widgetString, String rangeString, String valueString, String description, String minString, String maxString, ImmutableList<MenuItem> menuItems) {
        checkNotNull(name, "Name cannot be null.");
        checkNotNull(type, "Type cannot be null.");
        if (STANDARD_TYPES.contains(type)) {
            Object value;
            if (valueString == null) {
                value = DEFAULT_VALUES.get(type);
                checkNotNull(value);
            } else {
                if (type.equals("int")) {
                    value = Long.valueOf(valueString);
                } else if (type.equals("float")) {
                    value = Double.valueOf(valueString);
                } else if (type.equals("string")) {
                    value = valueString;
                } else if (type.equals("boolean")) {
                    value = Boolean.valueOf(valueString);
                } else if (type.equals("point")) {
                    value = Point.valueOf(valueString);
                } else if (type.equals("color")) {
                    value = Color.valueOf(valueString);
                } else {
                    throw new AssertionError("Unknown type " + type);
                }
            }
            Widget widget;
            if (widgetString != null && !widgetString.isEmpty()) {
                widget = parseWidget(widgetString);
            } else {
                widget = Widget.NONE;
            }
            Range range = rangeString != null ? parseRange(rangeString) : DEFAULT_RANGE;

            Double minimumValue = null;
            Double maximumValue = null;
            if (minString != null)
                minimumValue = Double.valueOf(minString);
            if (maxString != null)
                maximumValue = Double.valueOf(maxString);
            return new Port(name, type, label, null, widget, range, value, description, minimumValue, maximumValue, menuItems);
        } else {
            return Port.customPort(name, type);
        }
    }

    public static Widget defaultWidgetForType(String type) {
        checkNotNull(type, "Type cannot be null.");
        if (type.equals(TYPE_INT)) {
            return Widget.INT;
        } else if (type.equals(TYPE_FLOAT)) {
            return Widget.FLOAT;
        } else if (type.equals(TYPE_STRING)) {
            return Widget.STRING;
        } else if (type.equals(TYPE_BOOLEAN)) {
            return Widget.TOGGLE;
        } else if (type.equals(TYPE_POINT)) {
            return Widget.POINT;
        } else if (type.equals(TYPE_COLOR)) {
            return Widget.COLOR;
        } else {
            return Widget.NONE;
        }
    }

    private Port(String name, String type, Object value) {
        this(name, type, "", null, defaultWidgetForType(type), DEFAULT_RANGE, value, "", null, null, ImmutableList.<MenuItem>of());
    }

    private Port(String name, String type, Object value, Double minimumValue, Double maximumValue) {
        this(name, type, "", null, defaultWidgetForType(type), DEFAULT_RANGE, value, "", minimumValue, maximumValue, ImmutableList.<MenuItem>of());
    }

    private Port(String name, String type, Object value, Iterable<MenuItem> menuItems) {
        this(name, type, "", null, defaultWidgetForType(type), DEFAULT_RANGE, value, "", null, null, menuItems);
    }

    private Port(String name, String type, String label, String childReference, Widget widget, Range range, Object value, String description, Double minimumValue, Double maximumValue, Iterable<MenuItem> menuItems) {
        checkNotNull(name, "Name cannot be null.");
        checkNotNull(type, "Type cannot be null.");
        checkNotNull(menuItems, "Menu items cannot be null.");
        this.name = name;
        this.type = type;
        this.label = label;
        this.childReference = childReference;
        this.widget = widget;
        this.range = range;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
        this.value = clampValue(value);
        this.description = description;
        this.menuItems = ImmutableList.copyOf(menuItems);
        this.hashCode  = Objects.hashCode(name, type, value);
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getDisplayLabel() {
        if (label != null && ! label.isEmpty()) return label;
        return StringUtils.humanizeName(name);
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPublishedPort() {
        return childReference != null;
    }

    public String getChildReference() {
        return childReference;
    }

    public String getChildNodeName() {
        return childReference == null ? null : childReference.split("\\.")[0];
    }

    public String getChildPortName() {
        return childReference == null ? null : childReference.split("\\.")[1];
    }

    public Node getChildNode(Node network) {
        return network.getChild(getChildNodeName());
    }

    public Port getChildPort(Node network) {
        Node child = network.getChild(getChildNodeName());
        return child.getInput(getChildPortName());
    }

    public Range getRange() {
        return range;
    }

    public boolean hasValueRange() {
        return range.equals(Range.VALUE);
    }

    public boolean hasListRange() {
        return range.equals(Range.LIST);
    }

    public Double getMinimumValue() {
        return minimumValue;
    }

    public Double getMaximumValue() {
        return maximumValue;
    }

    public boolean hasMenu() {
        return !menuItems.isEmpty();
    }

    public List<MenuItem> getMenuItems() {
        return menuItems;
    }

    /**
     * Check if the Port type is a standard type, meaning it can be persisted, and its value can be accessed.
     *
     * @return true if this is a standard type.
     */
    public boolean isStandardType() {
        return STANDARD_TYPES.contains(type);
    }

    /**
     * Check if the Port type is a custom type.
     *
     * @return true if this is a custom type.
     */
    public boolean isCustomType() {
        return !isStandardType();
    }

    /**
     * Return the value stored in the port as a long.
     * <ul>
     * <li>Integers are returned as-is.</li>
     * <li>Floats are rounded using Math.round().</li>
     * <li>Other types return 0.</li>
     * </ul>
     *
     * @return The value as a long or 0 if the value cannot be converted.
     */
    public long intValue() {
        checkValueType();
        if (type.equals(TYPE_INT)) {
            return (Long) value;
        } else if (type.equals(TYPE_FLOAT)) {
            return Math.round((Double) value);
        } else {
            return 0L;
        }
    }

    /**
     * Return the value stored in the port as a Float.
     * <ul>
     * <li>Integers are converted to Floats.</li>
     * <li>Floats are returned as-is.</li>
     * <li>Other types return 0f.</li>
     * </ul>
     *
     * @return The value as a Float or 0f if the value cannot be converted.
     */
    public double floatValue() {
        checkValueType();
        if (type.equals(TYPE_INT)) {
            return ((Long) value).doubleValue();
        } else if (type.equals(TYPE_FLOAT)) {
            return (Double) value;
        } else {
            return 0.0;
        }
    }

    /**
     * Return the value stored in the port as a String.
     * <p/>
     * This conversion simply uses String.valueOf(), which does the right thing.
     *
     * @return The value as a String or "null" if the value is null. (for custom types)
     * @see String#valueOf(Object)
     */
    public String stringValue() {
        checkValueType();
        return String.valueOf(value);
    }

    /**
     * Return the value stored in the port as a boolean.
     * <p/>
     * If the port has a different type, false is returned.
     *
     * @return The value as a Float or 0f if the value cannot be converted.
     */
    public boolean booleanValue() {
        checkValueType();
        if (type.equals(TYPE_BOOLEAN)) {
            return (Boolean) value;
        } else {
            return false;
        }
    }

    /**
     * Return the value stored in the port as a Port.
     * <p/>
     * If the port has a different type, Point.ZERO is returned.
     *
     * @return The value as a Point or Point.ZERO if the value is of an incorrect type.
     */
    public Point pointValue() {
        checkValueType();
        if (type.equals(TYPE_POINT)) {
            return (Point) value;
        } else {
            return Point.ZERO;
        }
    }

    public Color colorValue() {
        checkValueType();
        if (type.equals(TYPE_COLOR)) {
            return (Color) value;
        } else {
            return Color.BLACK;
        }
    }

    /**
     * Return the value stored in the port as an Object.
     * <p/>
     * If this is a port with a custom type, this method returns null.
     *
     * @return The value as an Object or null.
     */
    public Object getValue() {
        checkValueType();
        return value;
    }

    //// Shim implementations of methods ////

    public boolean hasExpression() {
        return false;
    }

    public String getExpression() {
        return "";
    }

    public boolean isEnabled() {
        return true;
    }

    public Widget getWidget() {
        return widget;
    }

    public boolean isFileWidget() {
        return widget == Widget.FILE || widget == Widget.IMAGE;
    }

    //// Mutation methods ////

    public Port withLabel(String label) {
        return new Port(getName(), getType(), label, getChildReference(), getWidget(), getRange(), getValue(), getDescription(), getMinimumValue(), getMaximumValue(), getMenuItems());
    }

    public Port withDescription(String description) {
        return new Port(getName(), getType(), getLabel(), getChildReference(), getWidget(), getRange(), getValue(), description, getMinimumValue(), getMaximumValue(), getMenuItems());
    }

    public Port withChildReference(Node childNode, Port childPort) {
        checkNotNull(childNode);
        checkNotNull(childPort);
        String childReference = buildChildReference(childNode, childPort);
        return new Port(getName(), getType(), this.label, childReference, getWidget(), getRange(), getValue(), getDescription(), getMinimumValue(), getMaximumValue(), getMenuItems());
    }

    private static String buildChildReference(Node childNode, Port childPort) {
        checkNotNull(childNode);
        checkNotNull(childPort);
        return String.format("%s.%s", childNode.getName(), childPort.getName());
    }

    /**
     * Return a new Port with the value set to the given value.
     *
     * @param value The new value. This must be of the correct type.
     * @return The new Port.
     * @throws IllegalStateException If you're trying to change the value of a standard type, or you give the wrong value.
     */
    public Port withValue(Object value) {
        checkState(isStandardType(), "You can only change the value of a standard type.");
        checkArgument(correctValueForType(value), "Value '%s' is not correct for %s port.", value, getType());
        return new Port(getName(), getType(), getLabel(), getChildReference(), getWidget(), getRange(), clampValue(convertValue(getType(), value)), getDescription(), getMinimumValue(), getMaximumValue(), getMenuItems());
    }

    /**
     * Return a new Port with the widget set to the given widget value.
     *
     * @param widget The new widget.
     * @return The new Port.
     */
    public Port withWidget(Widget widget) {
        return new Port(getName(), getType(), getLabel(), getChildReference(), widget, getRange(), getValue(), getDescription(), getMinimumValue(), getMaximumValue(), getMenuItems());
    }

    /**
     * Return a new Port with the range set to the given range value.
     *
     * @param range The new range.
     * @return The new Port.
     */
    public Port withRange(Range range) {
        return new Port(getName(), getType(), getLabel(), getChildReference(), getWidget(), range, getValue(), getDescription(), getMinimumValue(), getMaximumValue(), getMenuItems());
    }

    /**
     * Convert integers to longs and floats to doubles. All other values are passed through as-is.
     *
     * @param type  The expected type.
     * @param value The original value.
     * @return The converted value.
     */
    private Object convertValue(String type, Object value) {
        if (value instanceof Integer) {
            checkArgument(type.equals(TYPE_INT));
            return (long) ((Integer) value);
        } else if (value instanceof Float) {
            checkArgument(type.equals(TYPE_FLOAT));
            return (double) ((Float) value);
        } else {
            return value;
        }
    }

    /**
     * Convert integers to longs and floats to doubles. All other values are passed through as-is.
     *
     * @param value The original value.
     * @return The converted value.
     */
    public Object clampValue(Object value) {
        if (getType().equals(TYPE_FLOAT)) {
            return clamp((Double) value);
        } else if (getType().equals(TYPE_INT)) {
            return (long) clamp(((Long) value).doubleValue());
        } else {
            return value;
        }
    }

    private double clamp(double v) {
        if (minimumValue != null && v < minimumValue) {
            return minimumValue;
        } else if (maximumValue != null && v > maximumValue) {
            return maximumValue;
        } else {
            return v;
        }
    }

    private void checkValueType() {
        checkState(correctValueForType(this.value), "The internal value %s is not a %s.", value, type);
    }

    private boolean correctValueForType(Object value) {
        if (type.equals(TYPE_INT)) {
            return value instanceof Long || value instanceof Integer;
        } else if (type.equals(TYPE_FLOAT)) {
            return value instanceof Double || value instanceof Float;
        } else if (type.equals(TYPE_STRING)) {
            return value instanceof String;
        } else if (type.equals(TYPE_BOOLEAN)) {
            return value instanceof Boolean;
        } else if (type.equals(TYPE_POINT)) {
            return value instanceof Point;
        } else if (type.equals(TYPE_COLOR)) {
            return value instanceof Color;
        } else {
            // The value of a custom type should always be null.
            return value == null;
        }
    }

    public Object getAttributeValue(Attribute attribute) {
        if (attribute == Attribute.NAME) {
            return getName();
        } else if (attribute == Attribute.TYPE) {
            return getType();
        } else if (attribute == Attribute.LABEL) {
            return getLabel();
        } else if (attribute == Attribute.DESCRIPTION) {
            return getDescription();
        } else if (attribute == Attribute.CHILD_REFERENCE) {
            return getChildReference();
        } else if (attribute == Attribute.WIDGET) {
            return getWidget();
        } else if (attribute == Attribute.RANGE) {
            return getRange();
        } else if (attribute == Attribute.MINIMUM_VALUE) {
            return getMinimumValue();
        } else if (attribute == Attribute.MAXIMUM_VALUE) {
            return getMaximumValue();
        } else if (attribute == Attribute.MENU_ITEMS) {
            return getMenuItems();
        } else {
            throw new AssertionError("Unknown port attribute " + attribute);
        }
    }

    public static Object parseValue(String type, String valueString) {
        if (type.equals("int")) {
            return Long.valueOf(valueString);
        } else if (type.equals("float")) {
            return Double.valueOf(valueString);
        } else if (type.equals("string")) {
            return valueString;
        } else if (type.equals("boolean")) {
            return Boolean.valueOf(valueString);
        } else if (type.equals("point")) {
            return Point.valueOf(valueString);
        } else if (type.equals("color")) {
            return Color.valueOf(valueString);
        } else {
            throw new AssertionError("Unknown type " + type);
        }
    }

    private static Widget parseWidget(String valueString) {
        return Widget.valueOf(valueString.toUpperCase(Locale.US));
    }

    private static Range parseRange(String valueString) {
        if (valueString.equals("value"))
            return Range.VALUE;
        else if (valueString.equals("list"))
            return Range.LIST;
        else
            throw new AssertionError("Unknown range " + valueString);
    }

    public Port withMinimumValue(Double minimumValue) {
        checkArgument(type.equals(Port.TYPE_INT) || type.equals(Port.TYPE_FLOAT),
                "You can only set a minimum value on int or float ports, not %s", this);
        return new Port(getName(), getType(), getLabel(), getChildReference(), getWidget(), getRange(), getValue(), getDescription(), minimumValue, getMaximumValue(), getMenuItems());
    }

    public Port withMaximumValue(Double maximumValue) {
        checkArgument(type.equals(Port.TYPE_INT) || type.equals(Port.TYPE_FLOAT),
                "You can only set a maximum value on int or float ports, not %s", this);
        return new Port(getName(), getType(), getLabel(), getChildReference(), getWidget(), getRange(), getValue(), getDescription(), getMinimumValue(), maximumValue, getMenuItems());
    }

    public Port withMenuItems(Iterable<MenuItem> items) {
        checkNotNull(items);
        checkArgument(type.equals(Port.TYPE_STRING), "You can only use menu items on string ports, not %s", this);
        return new Port(getName(), getType(), getLabel(), getChildReference(), getWidget(), getRange(), getValue(), getDescription(), getMinimumValue(), getMaximumValue(), items);
    }

    public Port withMenuItemAdded(String key, String label) {
        ImmutableList.Builder<MenuItem> b = ImmutableList.builder();
        b.addAll(menuItems);
        b.add(new MenuItem(key, label));
        return withMenuItems(b.build());
    }

    public Port withMenuItemRemoved(MenuItem menuItem) {
        ImmutableList.Builder<MenuItem> b = ImmutableList.builder();
        for (MenuItem item : menuItems) {
            if (item.equals(menuItem)) {
                // Do nothing
            } else {
                b.add(item);
            }
        }
        return withMenuItems(b.build());
    }

    public Port withMenuItemMovedUp(int index) {
        checkArgument(0 < index && index < menuItems.size());
        return withMenuItemMoved(index, index - 1);
    }

    public Port withMenuItemMovedDown(int index) {
        checkArgument(0 <= index && index < menuItems.size() - 1);
        return withMenuItemMoved(index, index + 1);
    }

    private Port withMenuItemMoved(int fromIndex, int toIndex) {
        List<MenuItem> items = new ArrayList<MenuItem>(0);
        items.addAll(menuItems);
        MenuItem item = items.get(fromIndex);
        items.remove(item);
        items.add(toIndex, item);
        return withMenuItems(ImmutableList.copyOf(items));
    }

    public Port withMenuItemChanged(int index, String key, String label) {
        checkArgument(0 <= index && index < menuItems.size());
        List<MenuItem> items = new ArrayList<MenuItem>(0);
        items.addAll(menuItems);
        items.set(index, new MenuItem(key, label));
        return withMenuItems(ImmutableList.copyOf(items));
    }

    public Port withParsedAttribute(Attribute attribute, String valueString) {
        checkNotNull(valueString);

        String name = this.name;
        String type = this.type;
        String label = this.label;
        String childReference = this.childReference;
        Widget widget = this.widget;
        Range range = this.range;
        Object value = this.value;
        String description = this.description;
        Double minimumValue = this.minimumValue;
        Double maximumValue = this.maximumValue;

        switch (attribute) {
            case LABEL:
                label = valueString;
                break;
            case DESCRIPTION:
                description = valueString;
                break;
            case CHILD_REFERENCE:
                childReference = valueString;
                break;
            case VALUE:
                checkArgument(STANDARD_TYPES.contains(type), "Port %s: you can only set the value for one of the standard types, not %s (value=%s)", name, type, valueString);
                value = parseValue(type, valueString);
                break;
            case WIDGET:
                widget = parseWidget(valueString);
                break;
            case RANGE:
                range = parseRange(valueString);
                break;
            case MINIMUM_VALUE:
                minimumValue = Double.valueOf(valueString);
                break;
            case MAXIMUM_VALUE:
                maximumValue = Double.valueOf(valueString);
                break;
            default:
                throw new AssertionError("You cannot use withParsedAttribute with attribute " + attribute);
        }
        return new Port(name, type, label, childReference, widget, range, value, description, minimumValue, maximumValue, getMenuItems());
    }

    //// Object overrides ////

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Port)) return false;
        final Port other = (Port) o;
        return Objects.equal(name, other.name)
                && Objects.equal(type, other.type)
                && Objects.equal(label, other.label)
                && Objects.equal(value, other.value)
                && Objects.equal(description, other.description)
                && Objects.equal(range, other.range);
    }

    @Override
    public String toString() {
        return String.format("<Port %s (%s): %s>", name, type, value);
    }

}
