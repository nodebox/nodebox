package nodebox.node;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class Device {
    public static final String TYPE_OSC = "osc";

    private final String name;
    private final String type;
    private final ImmutableMap<String, String> properties;

    private final transient int hashCode;

    public static Device oscDevice(String name, long port) {
        return new Device(name, TYPE_OSC, ImmutableMap.<String, String>of("port", String.valueOf(port)));
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

    public Map<String, String> getProperties() {
        return properties;
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
