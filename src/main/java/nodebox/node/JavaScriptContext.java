package nodebox.node;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import nodebox.function.FunctionLibrary;
import nodebox.function.FunctionRepository;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * A JavaScriptContext will render a node network using the JavaScript nodecore engine.
 */
public final class JavaScriptContext {

    private final NodeLibrary nodeLibrary;
    private final FunctionRepository functionRepository;
    private final Context context;
    private final Scriptable scope;

    public JavaScriptContext(NodeLibrary nodeLibrary) {
        this.nodeLibrary = nodeLibrary;
        this.functionRepository = nodeLibrary.getCombinedFunctionRepository();
        this.context = Context.enter();
        this.scope = context.initStandardObjects();

        // Load the core libraries
        loadSourceFromResource("underscore.js");
        loadSourceFromResource("nodecore.js");

        // Load libraries from the function repository
        for (FunctionLibrary library : functionRepository.getLibraries()) {
            if (library.getLanguage().equals("javascript")) {
                loadSourceFromFile(library.getFile());
            }
        }
    }

    private static String getSource(String resourceName) {
        try {
            URL url = Resources.getResource(resourceName);
            return Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<?> renderNetwork(Node node) {
        Scriptable nodecore = (Scriptable) scope.get("nodecore", scope);
        Function renderNodeFn = (Function) nodecore.get("evaluateNetwork", nodecore);
        NativeArray results = (NativeArray) renderNodeFn.call(context, scope, nodecore, new Object[]{node});
        return ImmutableList.of();
    }

    private Object loadSourceFromResource(String resourceName) {
        return context.evaluateString(scope, getSource(resourceName), resourceName, 1, null);
    }

    private Object loadSourceFromFile(File file) {
        try {
            return context.evaluateReader(scope, new FileReader(file), file.getName(), 1, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
