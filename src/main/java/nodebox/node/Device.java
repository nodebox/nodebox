package nodebox.node;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Device {
    public static final String TYPE_OSC = "osc";
    public static final String TYPE_AUDIOPLAYER = "audioplayer";
    public static final String TYPE_AUDIOINPUT = "audioinput";

    public static final String TIMELINE_SYNC = "sync_with_timeline";

    public static final ImmutableList<String> deviceTypes = ImmutableList.of(TYPE_OSC, TYPE_AUDIOPLAYER, TYPE_AUDIOINPUT);

    private final String name;
    private final String type;
    private final ImmutableMap<String, String> properties;

    private static final Pattern OSC_PROPERTY_NAMES_PATTERN = Pattern.compile("^(port|sync_with_timeline)$");
    private static final Pattern AUDIOPLAYER_PROPERTY_NAMES_PATTERN = Pattern.compile("^(filename|sync_with_timeline|loop)$");
    private static final Pattern AUDIOINPUT_PROPERTY_NAMES_PATTERN = Pattern.compile("^(sync_with_timeline)$");

    private final transient int hashCode;

    private static final Map<String, Pattern> validPropertyNames;

    static {
        ImmutableMap.Builder<String, Pattern> builder = new ImmutableMap.Builder<String, Pattern>();
        builder.put(TYPE_OSC, OSC_PROPERTY_NAMES_PATTERN);
        builder.put(TYPE_AUDIOPLAYER, AUDIOPLAYER_PROPERTY_NAMES_PATTERN);
        builder.put(TYPE_AUDIOINPUT, AUDIOINPUT_PROPERTY_NAMES_PATTERN);
        validPropertyNames = builder.build();
    }

    public static Device oscDevice(String name, long port, boolean syncWithTimeline) {
        return new Device(name, TYPE_OSC, ImmutableMap.<String, String>of("port", String.valueOf(port), TIMELINE_SYNC, String.valueOf(syncWithTimeline)));
    }

    public static Device deviceForType(String name, String type) {
        checkNotNull(type, "Type cannot be null.");
        checkArgument(deviceTypes.contains(type), "%s is not a valid device type.", type);
        // If the type is not found in the default values, get() returns null, which is what we need for custom types.
        return new Device(name, type, ImmutableMap.<String, String>of());
    }

    private Device(final String name, final String type, final ImmutableMap<String, String> properties) {
        this.name = name;
        this.type = type;
        this.properties = properties;
        this.hashCode  = Objects.hashCode(name, type, properties);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getProperty(String name, String defaultValue) {
        if (hasProperty(name)) {
            return properties.get(name);
        } else {
            return defaultValue;
        }
    }

    public Device withProperty(String name, String value) {
        checkArgument(isValidProperty(name), "Property name '%s' is not valid.", name);
        Map<String, String> b = new HashMap<String, String>();
        b.putAll(properties);
        b.put(name, value);
        return new Device(this.name, this.type, ImmutableMap.copyOf(b));
    }

    private boolean isValidProperty(String name) {
        checkNotNull(name);
        if (!validPropertyNames.containsKey(getType())) return false;
        return validPropertyNames.get(getType()).matcher(name).matches();
    }

    //// Object overrides ////

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Device)) return false;
        final Device other = (Device) o;
        return Objects.equal(name, other.name)
                && Objects.equal(type, other.type)
                && Objects.equal(properties, other.properties);
    }

    @Override
    public String toString() {
        return String.format("<Device %s (%s)>", name, type);
    }

}
