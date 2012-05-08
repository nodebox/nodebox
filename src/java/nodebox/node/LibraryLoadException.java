package nodebox.node;

/**
 * An exception that wraps the errors that occur during library loading.
 */
public class LibraryLoadException extends RuntimeException {

    private final String fileName;

    public LibraryLoadException(String fileName, Throwable t) {
        super("Error while loading " + fileName + ": " + t, t);
        this.fileName = fileName;
    }

    public LibraryLoadException(String fileName, String message) {
        super("Error while loading " + fileName + ": " + message);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
