package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;

public class PlatformUtils {
    public static int WIN = 1;
    public static int MAC = 2;
    public static int OTHER = 3;

    public static int current_platform = -1;
    public static int platformSpecificModifier;

    public static final String SEP = System.getProperty("file.separator");

    private static final String REG_QUERY_COMMAND = "reg query ";
    private static final String REG_STRING_TOKEN = "REG_SZ";
    private static final String REG_SHELL_FOLDERS = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders";
    private static final String REG_LOCAL_APPDATA = "Local AppData";

    private static File userDataDirectory = null;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        platformSpecificModifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        if (osName.indexOf("windows") != -1) {
            current_platform = WIN;
        } else if (osName.startsWith("mac os x")) {
            current_platform = MAC;
        } else {
            current_platform = OTHER;
        }
    }

    public static boolean onMac() {
        return current_platform == MAC;
    }

    public static boolean onWindows() {
        return current_platform == WIN;
    }

    public static boolean onOther() {
        return current_platform == OTHER;
    }

    //// Application directories ////

    public static File getHomeDirectory() {
        return new File(System.getProperty("user.home"));
    }

    /**
     * Get the directory that contains the user's NodeBox library directory.
     * <p/>
     * <p/>
     * <ul>
     * <li>Mac: <code>/Users/username/Library/NodeBox</code></li>
     * <li>Windows: <code>/Documents And Settings/username/Local Settings/Application Data/NodeBox</code></li>
     * <li>Other: <code>~/nodebox</code></li>
     * </ul>
     *
     * @return the user's library directory.
     */
    public static File getUserDataDirectory() throws RuntimeException {
        if (userDataDirectory != null)
            return userDataDirectory;
        if (onMac()) {
            userDataDirectory = new File(getHomeDirectory(), "Library/" + Application.NAME);
        } else if (onWindows()) {
            // Try to read the local application data from the system environment first.
            // This environment variable is only available on Windows Vista/7.
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData == null) {
                // If this fails, try to read the registry, which works on most systems, but is deprecated,
                // and has been known to be missing.
                localAppData = readWindowsRegistryValue(REG_SHELL_FOLDERS, REG_LOCAL_APPDATA);
                if (localAppData == null) {
                    // If reading the registry fails, use the home directory.
                    localAppData = getHomeDirectory().getPath();
                }
            }
            userDataDirectory = new File(localAppData, Application.NAME);
        } else {
            userDataDirectory = new File(getHomeDirectory(), Application.NAME.toLowerCase());
        }
        return userDataDirectory;
    }

    /**
     * Get the directory that contains NodeBox scripts the user has installed.
     * <p/>
     * <ul>
     * <li>Mac: <code>/Users/username/Library/NodeBox/Scripts</code></li>
     * <li>Windows: <code>/Users/username/Application Data/NodeBox/Scripts</code></li>
     * <li>Other: <code>~/nodebox/scripts</code></li>
     * </ul>
     *
     * @return the user's NodeBox scripts directory.
     */
    public static File getUserScriptsDirectory() {
        if (onMac() || onWindows())
            return new File(getUserDataDirectory(), "Scripts");
        else
            return new File(getUserDataDirectory(), "scripts");
    }

    /**
     * Get the directory that contains Pythhon libraries the user has installed.
     * <p/>
     * This directory is added to the PYTHONPATH; anything below it can be used in scripts.
     * <p/>
     * <ul>
     * <li>Mac: <code>/Users/username/Library/NodeBox/Python</code></li>
     * <li>Windows: <code>/Users/username/Application Data/NodeBox/Python</code></li>
     * <li>Other: <code>~/nodebox/python</code></li>
     * </ul>
     *
     * @return the user's Python directory.
     */
    public static File getUserPythonDirectory() {
        if (onMac() || onWindows())
            return new File(getUserDataDirectory(), "Python");
        else
            return new File(getUserDataDirectory(), "python");
    }

    /**
     * Get the directory that contains the application's builtin NodeBox scripts.
     *
     * @return the application's NodeBox scripts directory.
     */
    public static File getApplicationScriptsDirectory() {
        return new File("libraries");
    }

    //// Keystrokes ////

    public static KeyStroke getKeyStroke(int key) {
        return KeyStroke.getKeyStroke(key, platformSpecificModifier);
    }

    public static KeyStroke getKeyStroke(int key, int modifier) {
        return KeyStroke.getKeyStroke(key, platformSpecificModifier | modifier);
    }

    //// Registry ////

    public static String readWindowsRegistryValue(String key, String valueKey) {
        String command = REG_QUERY_COMMAND + " \"" + key + "\" /v \"" + valueKey + "\"";
        try {
            Process process = Runtime.getRuntime().exec(command);
            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            String result = reader.getResult();
            int p = result.indexOf(REG_STRING_TOKEN);
            if (p == -1)
                throw new RuntimeException("Cannot read Windows registry. Exiting...");
            return result.substring(p + REG_STRING_TOKEN.length()).trim();
        }
        catch (Exception e) {
            return null;
        }
    }

    public static void openURL(String url) {
        if (Desktop.isDesktopSupported()) {
            URI uri = URI.create(url);
            try {
                Desktop.getDesktop().browse(uri);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static private class StreamReader extends Thread {
        private InputStream is;
        private StringWriter sw;

        StreamReader(InputStream is) {
            this.is = is;
            sw = new StringWriter();
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1)
                    sw.write(c);
            }
            catch (IOException e) {
                throw new RuntimeException("Cannot read Windows registry. Exiting...", e);
            }
        }

        String getResult() {
            return sw.toString();
        }
    }

}
