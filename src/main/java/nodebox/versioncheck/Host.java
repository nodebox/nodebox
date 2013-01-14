package nodebox.versioncheck;

import java.net.URL;

/**
 * The host is the application or library that wants to allow updates.
 */
public interface Host {

    public String getName();

    public Version getVersion();

    public URL getIconFile();

    public String getAppcastURL();

}
