package nodebox.client;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;

public class PlatformUtils {
    public static int WIN = 1;
    public static int MAC = 2;
    public static int OTHER = 3;

    public static int current_platform = -1;
    public static int platformSpecificModifier;

    public static final String SEP = System.getProperty("file.separator");

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
     * <li>Windows: <code>/Users/username/Application Data/NodeBox</code></li>
     * <li>Other: <code>~/nodebox</code></li>
     * </ul>
     *
     * @return the user's library directory.
     */
    public static File getUserDataDirectory() {
        if (onMac()) {
            return new File(getHomeDirectory(), "Library/" + Application.NAME);
        } else if (onWindows()) {
            return new File(getHomeDirectory(), "Application Data/" + Application.NAME);
        } else {
            return new File(getHomeDirectory(), Application.NAME.toLowerCase());
        }
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

}
