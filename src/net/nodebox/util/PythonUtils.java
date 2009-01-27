package net.nodebox.util;

import net.nodebox.client.PlatformUtils;
import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;

import java.io.File;
import java.util.Properties;

public class PythonUtils {

    public static void initializePython() {
        Properties jythonProperties = new Properties();
        String jythonCacheDir = PlatformUtils.getUserDataDirectory() + PlatformUtils.SEP + "jythoncache";
        jythonProperties.put("python.cachedir", jythonCacheDir);
        PySystemState.initialize(System.getProperties(), jythonProperties, new String[]{""});
        String workingDirectory = System.getProperty("user.dir");
        File pythonLibraries = new File(workingDirectory, "lib" + PlatformUtils.SEP + "python.zip");
        Py.getSystemState().path.add(new PyString(pythonLibraries.getAbsolutePath()));
    }

}
