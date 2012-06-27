package nodebox.util;

import java.io.File;

/**
 * An exception that wraps the errors that occur during library loading.
 */
public class LoadException extends RuntimeException {

    private final File file;

    private static String errorMessage(File file, Object message) {
        return "Error while loading "
                + (file == null ? "unknown" : file.getName())
                + (message == null ? "." : ": " + message.toString());
    }

    public LoadException(File file, Throwable t) {
        super(errorMessage(file, t), t);
        this.file = file;
    }

    public LoadException(File file, String message) {
        super(errorMessage(file, message));
        this.file = file;
    }

    public LoadException(File file, String s, Throwable throwable) {
        super(s, throwable);
        this.file = file;
    }

    public File getFile() {
        return file;
    }

}
