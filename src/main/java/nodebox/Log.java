package nodebox;

import nodebox.util.AppDirs;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public class Log {
    private static final Logger LOGGER = java.util.logging.Logger.getGlobal();

    static {
        try {
            // Initialize logging directory
            AppDirs.ensureUserLogDir();
            File logDir = AppDirs.getUserLogDir();

            // Initialize file logging
            FileHandler handler = new FileHandler(logDir.getAbsolutePath() + "/nodebox-%u.log");
            handler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void info(String message) {
        LOGGER.log(Level.INFO, message);
    }

    public static void warn(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    public static void warn(String message, Throwable t) {
        LOGGER.log(Level.WARNING, message, t);
    }

    public static void error(String message) {
        LOGGER.log(Level.SEVERE, message);
    }

    public static void error(String message, Throwable t) {
        LOGGER.log(Level.SEVERE, message, t);
    }

}
