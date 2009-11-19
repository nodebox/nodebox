package nodebox.versioncheck;

/**
 * The host is the application or library that wants to allow updates.
 */
public interface Host {

    public String getName();

    public Version getVersion();

    public String getIconFile();

    public String getAppcastURL();

}
