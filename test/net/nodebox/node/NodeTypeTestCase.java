package net.nodebox.node;

import junit.framework.TestCase;
import net.nodebox.client.PlatformUtils;
import org.python.core.PySystemState;

import java.util.Properties;

public class NodeTypeTestCase extends TestCase {

    protected void initJython() {
        Properties jythonProperties = new Properties();
        String jythonCacheDir = PlatformUtils.getUserDataDirectory() + PlatformUtils.SEP + "jythoncache";
        jythonProperties.put("python.cachedir", jythonCacheDir);
        PySystemState.initialize(System.getProperties(), jythonProperties, new String[]{""});
    }

    public String getLibrariesDirectory() {
        String projectDirectory = System.getProperty("user.dir");
        return projectDirectory + PlatformUtils.SEP + "libraries";
    }

    public void testDummy() {
        // This needs to be here, otherwise jUnit complains that there are no tests in this class.
    }

}
