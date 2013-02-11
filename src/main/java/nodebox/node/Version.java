package nodebox.node;

import com.google.common.base.Objects;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^([0-9]+\\.){0,2}[0-9]+$");
    private final int major, minor, revision;

    public Version() {
        major = 1;
        minor = 0;
        revision = 0;
    }

    public Version(int major, int minor) {
        this(major, minor, 0);
    }

    public Version(int major, int minor, int revision) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
    }

    public Version(String versionString) throws IllegalArgumentException {
        Matcher m = VERSION_PATTERN.matcher(versionString);
        if (!m.matches()) {
            throw new IllegalArgumentException("Version string " + versionString + " is not a valid version (expecting 1.0.0)");
        }
        String[] bits = versionString.split("\\.");
        if (bits.length == 0) {
            major = 0;
            minor = 0;
            revision = 0;
        } else if (bits.length == 1) {
            major = Integer.parseInt(bits[0]);
            minor = 0;
            revision = 0;
        } else if (bits.length == 2) {
            major = Integer.parseInt(bits[0]);
            minor = Integer.parseInt(bits[1]);
            revision = 0;
        } else {
            major = Integer.parseInt(bits[0]);
            minor = Integer.parseInt(bits[1]);
            revision = Integer.parseInt(bits[2]);
        }
    }

    public static Version parseVersionString(String s) throws IllegalArgumentException {
        return new Version(s);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getRevision() {
        return revision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Version)) return false;
        Version v = (Version) o;
        return major == v.major &&
                minor == v.minor &&
                revision == v.revision;
    }

    public int hashCode() {
        return Objects.hashCode(major, minor, revision);
    }

    public boolean largerThan(Version other) {
        if (major > other.major) return true;
        if (major == other.major && minor > other.minor) return true;
        if (major == other.major && minor == other.minor && revision > other.revision) return true;
        return false;
    }

    public boolean largerOrEqualThan(Version other) {
        if (largerThan(other)) return true;
        return major == other.major && minor == other.minor && revision == other.revision;
    }

    public boolean smallerOrEqualThan(Version other) {
        return !largerThan(other);
    }

    public boolean smallerThan(Version other) {
        return !largerOrEqualThan(other);
    }

    public String toString() {
        return major + "." + minor + "." + revision;
    }

}