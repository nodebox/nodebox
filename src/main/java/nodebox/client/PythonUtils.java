package nodebox.client;

import nodebox.ui.Platform;
import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PythonUtils {

    public static final File libDir;

    static {
        final File localDir = new File("lib");
        if (localDir.isDirectory()) {
            libDir = localDir;
        } else {
            libDir = nodebox.util.FileUtils.getApplicationFile("lib");
        }
    }

    static AtomicBoolean isInitialized = new AtomicBoolean(false);

    public synchronized static void initializePython() {
        if (isInitialized.get()) return;

        // Set the Jython package cache directory.
        Properties jythonProperties = new Properties();
        String jythonCacheDir = Platform.getUserDataDirectory() + Platform.SEP + "_jythoncache";
        jythonProperties.put("python.cachedir", jythonCacheDir);

        // Initialize Python.
        PySystemState.initialize(System.getProperties(), jythonProperties, new String[]{""});

        // Add the built-in Python libraries.
        File nodeBoxLibraries = new File(libDir, "nodeboxlibs.zip");
        Py.getSystemState().path.add(new PyString(nodeBoxLibraries.getAbsolutePath()));

        // This folder contains unarchived NodeBox libraries.
        // Only used in development.
        File developmentLibraries = new File("src/main/python");
        Py.getSystemState().path.add(new PyString(developmentLibraries.getAbsolutePath()));

        // Add the user's Python directory.
        Py.getSystemState().path.add(new PyString(Platform.getUserPythonDirectory().getAbsolutePath()));

        isInitialized.set(true);
    }

}
