package nodebox.ui;

import javax.swing.*;
import java.util.Locale;

public class LastResortHandler implements Thread.UncaughtExceptionHandler {

    public static void handle(Throwable t) {
        System.out.println("handle!");
        showException(Thread.currentThread(), t);
    }

    public void uncaughtException(final Thread t, final Throwable e) {
        if (SwingUtilities.isEventDispatchThread()) {
            showException(t, e);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    showException(t, e);
                }
            });
        }
    }

    private static void showException(Thread t, Throwable e) {
        String msg = String.format(Locale.US, "Unexpected problem on thread %s: %s",
                t.getName(), e.getMessage());

        logException(t, e);

        // note: in a real app, you should locate the currently focused frame
        // or dialog and use it as the parent. In this example, I'm just passing
        // a null owner, which means this dialog may get buried behind
        // some other screen.
        ExceptionDialog ed = new ExceptionDialog(null, e);
        ed.setVisible(true);
    }

    private static void logException(Thread t, Throwable e) {
        // todo: start a thread that sends an email, or write to a log file, or
        // send a JMS message...whatever
    }
}
