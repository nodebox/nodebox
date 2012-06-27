package nodebox.node;

import nodebox.util.LoadException;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: fdb
 * Date: 21/06/12
 * Time: 13:38
 * To change this template use File | Settings | File Templates.
 */
public class OutdatedLibraryException extends LoadException {

    public OutdatedLibraryException(File file, Throwable t) {
        super(file, t);
    }

    public OutdatedLibraryException(File file, String message) {
        super(file, message);
    }

    public OutdatedLibraryException(File file, String s, Throwable throwable) {
        super(file, s, throwable);
    }
}
