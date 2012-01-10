package nodebox.versioncheck;

public class Version implements Comparable {

    private static final int LARGER_THAN = 1;
    private static final int SMALLER_THAN = -1;
    private static final int EQUAL = 0;

    private String versionString;

    public Version(String version) {
        versionString = version;
    }

    @Override
    public String toString() {
        return versionString;
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    public int compareTo(Object o) {
        if (!(o instanceof Version)) return -1;
        Version other = (Version) o;

        String[] thisParts = this.versionString.split("\\.");
        String[] otherParts = other.versionString.split("\\.");

        if (this.versionString.equals(other.versionString))
            return EQUAL;

        int partCount = Math.min(thisParts.length, otherParts.length);
        for (int i = 0; i < partCount; i++) {
            String thisPart = thisParts[i];
            String otherPart = otherParts[i];
            Integer thisPartInt = parseInt(thisPart);
            Integer otherPartInt = parseInt(otherPart);
            if (thisPartInt != null && otherPartInt != null) {
                // Two numbers, can compare.
                if (thisPartInt > otherPartInt) {
                    return LARGER_THAN;
                } else if (thisPartInt < otherPartInt) {
                    return SMALLER_THAN;
                }
            } else {
                if (thisPartInt == null && otherPartInt != null) {
                    // This is a string, the other part is a number.
                    // String wins.
                    return LARGER_THAN;
                } else if (thisPartInt != null && otherPartInt == null) {
                    // This is a number, the other part is not.
                    // String wins.
                    return SMALLER_THAN;
                } else {
                    // Two strings. Compare them.
                    int comparison = thisPart.compareTo(otherPart);
                    // Only return if they are equal, otherwise keep on checking.
                    if (comparison != 0) {
                        return comparison;
                    }
                }
            }
        }

        // We're still here. Version string with most segments wins.
        if (thisParts.length > otherParts.length) {
            return LARGER_THAN;
        } else if (thisParts.length < otherParts.length) {
            return SMALLER_THAN;
        } else {
            throw new AssertionError("Parts appear to be equal but they are not. " + this.versionString + " -- " + other.versionString);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version) o;

        if (!versionString.equals(version.versionString)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return versionString.hashCode();
    }
}
