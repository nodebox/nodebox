package nodebox.ui;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeMapped;
import com.sun.jna.PointerType;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import nodebox.client.Application;

public class Platform {
    public static final int WIN = 1;
    public static final int MAC = 2;
    public static final int OTHER = 3;

    public static final int current_platform;
    public static final int platformSpecificModifier;

    public static final String SEP = System.getProperty("file.separator");

    private static File userDataDirectory = null;
    private static Map<String, Object> JNA_OPTIONS = new HashMap<String, Object>();

    static {
        if (!GraphicsEnvironment.isHeadless()) {
            platformSpecificModifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        } else {
            platformSpecificModifier = Event.CTRL_MASK;
        }
        if (com.sun.jna.Platform.isWindows()) {
            current_platform = WIN;
            JNA_OPTIONS.put(Library.OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
            JNA_OPTIONS.put(Library.OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
        } else if (com.sun.jna.Platform.isMac()) {
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
     * <li>Linux/BSD/Other: <code>~/.local/share/nodebox</code></li>
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
            String localAppData;
            HWND hwndOwner = null;
            int nFolder = Shell32.CSIDL_LOCAL_APPDATA;
            HANDLE hToken = null;
            int dwFlags = Shell32.SHGFP_TYPE_CURRENT;
            char[] pszPath = new char[Shell32.MAX_PATH];
            int hResult = Shell32.INSTANCE.SHGetFolderPath(hwndOwner, nFolder, hToken, dwFlags, pszPath);
            if (Shell32.S_OK == hResult) {
                String path = new String(pszPath);
                int len = path.indexOf('\0');
                localAppData = path.substring(0, len);
            } else {
                // If the native call fails, use the home directory.
                localAppData = getHomeDirectory().getPath();
            }
            userDataDirectory = new File(localAppData, Application.NAME);
        } else {
            userDataDirectory = new File(getHomeDirectory(), ".local/share/" + Application.NAME.toLowerCase(Locale.US));
        }
        return userDataDirectory;
    }

    /**
     * Get the directory that contains NodeBox scripts the user has installed.
     * <p/>
     * <ul>
     * <li>Mac: <code>/Users/username/Library/NodeBox/Scripts</code></li>
     * <li>Windows: <code>/Users/username/Application Data/NodeBox/Scripts</code></li>
     * <li>Linux/BSD/Other: <code>~/.local/share/nodebox/scripts</code></li>
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
     * Get the directory that contains Python libraries the user has installed.
     * <p/>
     * This directory is added to the PYTHONPATH; anything below it can be used in scripts.
     * <p/>
     * <ul>
     * <li>Mac: <code>/Users/username/Library/NodeBox/Python</code></li>
     * <li>Windows: <code>/Users/username/Application Data/NodeBox/Python</code></li>
     * <li>Linux/BSD/Other: <code>~/.local/share/nodebox/python</code></li>
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

    public static void openURL(String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not open browser window. Go to " + url + " directly. \n" + e.getLocalizedMessage());
        }
    }

    private static class HANDLE extends PointerType implements NativeMapped {
    }

    private static class HWND extends HANDLE {
    }

    private static interface Shell32 extends Library {

        public static final int MAX_PATH = 260;
        public static final int CSIDL_LOCAL_APPDATA = 0x001c;
        public static final int SHGFP_TYPE_CURRENT = 0;
        public static final int S_OK = 0;

        static Shell32 INSTANCE = (Shell32) Native.loadLibrary("shell32", Shell32.class, JNA_OPTIONS);

        /**
         * See http://msdn.microsoft.com/en-us/library/bb762181(VS.85).aspx
         * <p/>
         *
         * @param hwndOwner [in] Reserved.
         * @param nFolder   [in] A CSIDL value that identifies the folder whose path is to be retrieved.
         * @param hToken    [in] An access token that can be used to represent a particular user. Always set this to null.
         * @param dwFlags   [in] Flags that specify the path to be returned.
         * @param pszPath   [out] A pointer to a null-terminated string of length MAX_PATH which will receive the path.
         * @return S_OK if successful, or an error value otherwise.
         */
        public int SHGetFolderPath(HWND hwndOwner, int nFolder, HANDLE hToken,
                                   int dwFlags, char[] pszPath);
    }


}
