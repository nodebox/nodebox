package nodebox.util;

/**
 * An exception that wraps the errors that occur during library loading.
 */
public class LoadException extends RuntimeException {

    private final String fileName;

    public LoadException(String fileName, Throwable t) {
        super("Error while loading " + fileName + ": " + t, t);
        this.fileName = fileName;
    }

    public LoadException(String fileName, String message) {
        super("Error while loading " + fileName + ": " + message);
        this.fileName = fileName;
    }

    public LoadException(String fileName, String s, Throwable throwable) {
        super(s, throwable);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

}
