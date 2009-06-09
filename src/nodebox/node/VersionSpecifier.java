package nodebox.node;

/**
 * A "fuzzy" way of specifying a node version.
 * <p/>
 * Examples:
 * <ul>
 * <li>"=2.0" -- only version 2.0, nothing above or below that.</li>
 * <!-- <li>"2.0" -- same as above. Used for XML parsing.</li> -->
 * <li>"&gt;=2.0" -- anything greater or equal then version 2.0</li>
 * </ul>
 */
public class VersionSpecifier {

    private String specifier;

    public VersionSpecifier(String specifier) {
        this.specifier = specifier;
    }

    public String getSpecifier() {
        return specifier;
    }

    public boolean matches(Version version) {
        // TODO: implement
        return false;
    }

    public boolean matches(int major, int minor) {
        return matches(new Version(major, minor));
    }
}
