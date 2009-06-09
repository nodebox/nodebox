package nodebox.client;

import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;

import java.io.File;
import java.util.Properties;

public class PythonUtils {

    public static void initializePython() {
        // Set the Jython package cache directory.
        Properties jythonProperties = new Properties();
        String jythonCacheDir = PlatformUtils.getUserDataDirectory() + PlatformUtils.SEP + "_jythoncache";
        jythonProperties.put("python.cachedir", jythonCacheDir);

        // Initialize Python.
        PySystemState.initialize(System.getProperties(), jythonProperties, new String[]{""});

        // Add the built-in Python libraries.
        String workingDirectory = System.getProperty("user.dir");
        File pythonLibraries = new File(workingDirectory, "lib" + PlatformUtils.SEP + "python.zip");
        Py.getSystemState().path.add(new PyString(pythonLibraries.getAbsolutePath()));

        // Add the user's Python directory.
        Py.getSystemState().path.add(new PyString(PlatformUtils.getUserPythonDirectory().getAbsolutePath()));
    }

}
