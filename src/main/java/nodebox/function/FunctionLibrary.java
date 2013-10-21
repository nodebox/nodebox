package nodebox.function;

import com.google.common.base.Objects;
import nodebox.util.LoadException;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * A collection of functions. This collection is contained in a namespace.
 */
public abstract class FunctionLibrary {

    private static final Pattern HREF_PATTERN = Pattern.compile("^([a-z]+):(.*)$");

    public static FunctionLibrary load(String href) {
        return load(null, href);
    }

    public static FunctionLibrary load(File file, String href) {
        Matcher hrefMatcher = HREF_PATTERN.matcher(href);
        checkArgument(hrefMatcher.matches(), "Library identifier should be in the form language:filename.ext");
        checkState(hrefMatcher.groupCount() == 2);
        String language = hrefMatcher.group(1);
        String identifier = hrefMatcher.group(2);
        if (file != null && file.isFile())
            file = file.getParentFile();
        if (language.equals("java")) {
            return JavaLibrary.loadStaticClass(identifier);
        } else if (language.equals("clojure")) {
            return ClojureLibrary.loadScript(file, identifier);
        } else if (language.equals("python")) {
            return PythonLibrary.loadScript(file, identifier);
        } else if (language.equals("javascript")) {
            return JavaScriptLibrary.loadScript(file, identifier);
        } else {
            throw new LoadException(file, "Unknown function library type " + language + ".");
        }
    }

    public static String parseLanguage(String href) {
        Matcher hrefMatcher = HREF_PATTERN.matcher(href);
        checkArgument(hrefMatcher.matches(), "Library identifier should be in the form language:filename.ext");
        checkState(hrefMatcher.groupCount() == 2);
        return hrefMatcher.group(1);
    }

    public static FunctionLibrary ofClass(String namespace, Class c, String... methodNames) {
        return JavaLibrary.ofClass(namespace, c, methodNames);
    }

    public abstract String getSimpleIdentifier();

    public abstract String getNamespace();

    public abstract String getLanguage();

    /**
     * Get the name of the library as it should be written when linking to this library.
     * <p/>
     * The format is "language:[filepath]filename.extension" ie. "clojure:voronoi.clj".
     *
     * @return A link to a library.
     */
    public String getLink() {
        return getLink(null);
    }

    /**
     * Get the name of the library as it should be written when linking to this library.
     * <p/>
     * The format is "language:[filepath]filename.extension" ie. "clojure:voronoi.clj".
     *
     * @param baseFile The file to which the path of this library is relative to.
     * @return A link to a library.
     */
    public abstract String getLink(File baseFile);

    /**
     * Get the file that contains the library's code.
     *
     * @return the File or null if the code is not contained in a file.
     */
    public abstract File getFile();

    public abstract Function getFunction(String name);

    public abstract boolean hasFunction(String name);

    public void reload() {
    }


    //// Object overrides ////

    @Override
    public int hashCode() {
        return Objects.hashCode(getNamespace());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FunctionLibrary)) return false;
        final FunctionLibrary other = (FunctionLibrary) o;
        return Objects.equal(getLink(), other.getLink());
    }

    @Override
    public String toString() {
        return String.format("<FunctionLibrary %s>", getNamespace());
    }

}
