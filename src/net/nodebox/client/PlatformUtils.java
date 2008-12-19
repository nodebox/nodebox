package net.nodebox.client;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class PlatformUtils {
    public static int WIN = 1;
    public static int MAC = 2;
    public static int OTHER = 3;

    public static int current_platform = -1;
    public static int platformSpecificModifier;
    private static Border platformLineBorder;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        platformSpecificModifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        if (osName.indexOf("windows") != -1) {
            current_platform = WIN;
        } else if (osName.indexOf("mac") != -1) {
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

    public static KeyStroke getKeyStroke(int key) {
        return KeyStroke.getKeyStroke(key, platformSpecificModifier);
    }

    public static KeyStroke getKeyStroke(int key, int modifier) {
        return KeyStroke.getKeyStroke(key, platformSpecificModifier | modifier);
    }

    public static Font getEditorFont() {
        if (onMac()) {
            return new Font("Monaco", Font.PLAIN, 10);
        } else {
            return new Font("Courier", Font.PLAIN, 12);
        }
    }

    public static Font getMessageFont() {
        if (onMac()) {
            return new Font("Lucida Grande", Font.BOLD, 13);
        } else {
            return new Font("Verdana", Font.BOLD, 11);
        }
    }

    public static Font getInfoFont() {
        if (onMac()) {
            return new Font("Lucida Grande", Font.PLAIN, 11);
        } else {
            return new Font("Verdana", Font.PLAIN, 10);
        }
    }

    public static Font getSmallFont() {
        if (onMac()) {
            return new Font("Lucida Grande", Font.PLAIN, 11);
        } else {
            return new Font("Verdana", Font.PLAIN, 10);
        }
    }

    public static Border createLineBorder() {
        if (platformLineBorder == null) {
            Color borderColor;
            if (onWindows()) {
                borderColor = new Color(100, 100, 100);
            } else if (onMac()) {
                borderColor = new Color(200, 200, 200);
            } else {
                borderColor = new Color(200, 200, 200);
            }
            platformLineBorder = BorderFactory.createLineBorder(borderColor);
        }
        return platformLineBorder;
    }

}
