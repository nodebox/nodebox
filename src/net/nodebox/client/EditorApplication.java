package net.nodebox.client;

import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The NodeBox "classic" version.
 * <p/>
 * Eventually both Application and EditorApplication will be merge, whereby the classic version will become a
 * set of panel settings.
 */
public class EditorApplication {

    public static final String NAME = "NodeBox";

    private static EditorApplication instance;
    private List<EditorDocument> documents = new ArrayList<EditorDocument>();
    private EditorDocument currentDocument;

    public static EditorApplication getInstance() {
        return instance;
    }

    private EditorApplication() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        // Initialize Jython
        Properties jythonProperties = new Properties();
        String jythonCacheDir = PlatformUtils.getUserDataDirectory() + PlatformUtils.SEP + "jythoncache";
        jythonProperties.put("python.cachedir", jythonCacheDir);
        PySystemState.initialize(System.getProperties(), jythonProperties, new String[]{""});
        String workingDirectory = System.getProperty("user.dir");
        File pythonLibraries = new File(workingDirectory, "lib" + PlatformUtils.SEP + "python.zip");
        Py.getSystemState().path.add(new PyString(pythonLibraries.getAbsolutePath()));
        Py.getSystemState().path.add(new PyString(PlatformUtils.getUserDataDirectory()));
        createNewDocument();
    }

    public EditorDocument createNewDocument() {
        EditorDocument doc = new EditorDocument();
        doc.setVisible(true);
        documents.add(doc);
        currentDocument = doc;
        return doc;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                instance = new EditorApplication();
            }
        });
    }

}
