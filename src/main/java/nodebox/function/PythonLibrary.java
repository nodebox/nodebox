package nodebox.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.client.PythonUtils;
import nodebox.util.FileUtils;
import nodebox.util.LoadException;
import org.python.core.*;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

public class PythonLibrary extends FunctionLibrary {

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[a-z0-9_]+\\.py");

    /**
     * Given a file name, determines the namespace.
     *
     * @param fileName The file name. Should end in ".py".
     * @return The namespace.
     */
    public static String namespaceForFile(String fileName) {
        checkArgument(fileName.endsWith(".py"), "The file name of a Python library needs to end in .py (not %s)", fileName);
        checkArgument(fileName.trim().length() >= 4, "The file name can not be empty (was %s).", fileName);
        File f = new File(fileName);
        String baseName = f.getName();
        checkArgument(FILE_NAME_PATTERN.matcher(baseName).matches(), "The file name can only contain lowercase letters, numbers and underscore (was %s).", fileName);
        return baseName.substring(0, baseName.length() - 3);
    }

    /**
     * Load the Python module.
     * <p/>
     * The namespace is determined automatically by using the file name.
     *
     * @param baseFile The file to which the path of this library is relative to.
     * @param fileName The file name.
     * @return The new Python library.
     * @throws LoadException If the script could not be loaded.
     * @see #namespaceForFile(String)
     */
    public static PythonLibrary loadScript(File baseFile, String fileName) throws LoadException {
        return loadScript(namespaceForFile(fileName), baseFile, fileName);
    }

    /**
     * Load the Python module.
     *
     * @param namespace The name space in which the library resides.
     * @param fileName  The file name.
     * @return The new Python library.
     * @throws LoadException If the script could not be loaded.
     */
    public static PythonLibrary loadScript(String namespace, String fileName) throws LoadException {
        return loadScript(namespace, null, fileName);
    }

    /**
     * Load the Python module.
     *
     * @param namespace The name space in which the library resides.
     * @param baseFile  The file to which the path of this library is relative to.
     * @param fileName  The file name.
     * @return The new Python library.
     * @throws LoadException If the script could not be loaded.
     */
    public static PythonLibrary loadScript(String namespace, File baseFile, String fileName) throws LoadException {
        File file;
        if (baseFile != null) {
            file = new File(baseFile, fileName);
        } else {
            file = new File(fileName);
        }
        if (!file.exists()) {
            throw new LoadException(file, "Library does not exist.");
        }
        return new PythonLibrary(namespace, file, loadScript(file));
    }

    private static Future<ImmutableMap<String, Function>> loadScript(final File file) {
        FutureTask<ImmutableMap<String, Function>> task = new FutureTask<ImmutableMap<String, Function>>(new Callable<ImmutableMap<String, Function>>() {
            public ImmutableMap<String, Function> call() throws Exception {
                // This creates a dependency between function and the client.
                // However, we need to know the load paths before we can do anything, so this is necessary.
                PythonUtils.initializePython();
                Py.getSystemState().path.append(new PyString(file.getParentFile().getCanonicalPath()));
                PythonInterpreter interpreter = new PythonInterpreter();
                try {
                    interpreter.execfile(file.getCanonicalPath());
                } catch (IOException e) {
                    throw new LoadException(file, e);
                } catch (PyException e) {
                    throw new LoadException(file, e);
                }
                PyStringMap map = (PyStringMap) interpreter.getLocals();

                ImmutableMap.Builder<String, Function> builder = ImmutableMap.builder();

                for (Object key : map.keys()) {
                    Object o = map.get(Py.java2py(key));
                    if (o instanceof PyFunction) {
                        String name = (String) key;
                        Function f = new PythonFunction(name, (PyFunction) o);
                        builder.put(name, f);
                    }
                }
                return builder.build();
            }
        });
        Thread t = new Thread(task);
        t.start();
        return task;
    }

    private final String namespace;
    private final File file;
    private Future<ImmutableMap<String, Function>> functionMap;

    private PythonLibrary(String namespace, File file, Future<ImmutableMap<String, Function>> functionMap) {
        this.namespace = namespace;
        this.file = file;
        this.functionMap = functionMap;
    }

    @Override
    public String getLink(File baseFile) {
        File parentFile = baseFile != null ? baseFile.getParentFile() : null;
        return "python:" + FileUtils.getRelativeLink(file, parentFile);
    }

    public String getSimpleIdentifier() {
        return file.getName();
    }

    public String getNamespace() {
        return namespace;
    }

    public String getLanguage() {
        return "python";
    }

    public File getFile() {
        return file;
    }

    public ImmutableMap<String, Function> getFunctionMap() {
        try {
            return functionMap.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Function getFunction(String name) {
        return getFunctionMap().get(name);
    }

    public boolean hasFunction(String name) {
        return getFunctionMap().containsKey(name);
    }

    /**
     * Reloads the python module.
     */
    @Override
    public void reload() {
        this.functionMap = loadScript(this.file);
        // Because we don't want this to happen asynchronously, get the results of the functionMap immediately.
        try {
            this.functionMap.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class PythonFunction implements Function {

        private final String name;
        private final PyFunction fn;

        public PythonFunction(String name, PyFunction fn) {
            this.name = name;
            this.fn = fn;
        }

        public String getName() {
            return name;
        }

        public Object invoke(Object... args) throws Exception {
            PyObject[] pyArgs = new PyObject[args.length];
            for (int i = 0; i < args.length; i++)
                pyArgs[i] = Py.java2py(args[i]);

            PyObject pyResult = fn.__call__(pyArgs);
            if (pyResult == null)
                return null;
            // todo: number conversions should be handled higher up in the code, and not at the Jython level.
            if (pyResult instanceof PyLong || pyResult instanceof PyInteger)
                return pyResult.__tojava__(Long.class);

            Object result = pyResult.__tojava__(Object.class);
            if (result == Py.NoConversion)
                throw new RuntimeException("Cannot convert Python object " + pyResult + " to java.");
            return result;
        }

        public ImmutableList<Argument> getArguments() {
            // todo: check if keeping a list of arguments makes sense in a python environment.
            return ImmutableList.of();
        }
    }
}
