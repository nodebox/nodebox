package nodebox.util;

import java.io.File;
import java.io.IOException;

public class AppDirs {

    static final PlatformAppDirs platform;
    static final String separator = System.getProperty("file.separator");
    static final String homeDir = System.getProperty("user.home");
    static final String appName = "NodeBox";

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("mac os")) {
            platform = new MacOsAppDirs();
        } else if (os.startsWith("windows")) {
            platform = new WindowsAppDirs();
        } else {
            platform = new LinuxAppDirs();
        }
    }

    interface PlatformAppDirs {
        File getUserDataDir();

        File getUserLogDir();
    }

    static class MacOsAppDirs implements PlatformAppDirs {

        @Override
        public File getUserDataDir() {
            return new File(homeDir + "/Library/Application Support/" + appName);
        }

        @Override
        public File getUserLogDir() {
            return new File(homeDir + "/Library/Logs/" + appName);
        }
    }

    static class WindowsAppDirs implements PlatformAppDirs {

        @Override
        public File getUserDataDir() {
            return new File(System.getenv("APPDATA") + separator + appName);
        }


        @Override
        public File getUserLogDir() {
            return new File(getUserDataDir(), "Logs");
        }
    }

    static class LinuxAppDirs implements PlatformAppDirs {

        @Override
        public File getUserDataDir() {
            String path = getEnv("XDG_DATA_HOME", homeDir + "/.local/share");
            return new File(path, appName);
        }

        public boolean ensureUserDataDir() {
            return getUserDataDir().mkdirs();
        }

        @Override
        public File getUserLogDir() {
            return new File(getUserDataDir(), "logs");
        }

        private String getEnv(String key, String defaultValue) {
            String value = System.getenv(key);
            if (value != null) {
                return value;
            } else {
                return defaultValue;
            }
        }

    }

    public static File getUserDataDir() {
        return platform.getUserDataDir();
    }

    public static File getUserLogDir() {
        return platform.getUserLogDir();
    }

    public static boolean ensureUserDataDir() {
        return platform.getUserDataDir().mkdirs();
    }

    public static boolean ensureUserLogDir() {
        return platform.getUserLogDir().mkdirs();
    }

}
