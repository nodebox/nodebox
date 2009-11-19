package nodebox.versioncheck;

import nodebox.client.SwingUtils;

import java.awt.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Main class. Checks for updates.
 */
public class Updater {

    public static final String LAST_UPDATE_CHECK = "FBLastUpdateCheck";
    public static final long UPDATE_INTERVAL = 1000 * 60 * 60 * 24; // 1000 milliseconds * 60 seconds * 60 minutes * 24 hours = Every day

    private final Host host;
    private boolean automaticCheck;
    private Preferences preferences;
    private UpdateChecker updateChecker;
    private UpdateDelegate delegate;
    private UpdateCheckDialog updateCheckDialog;

    public Updater(Host host) {
        this.host = host;
        automaticCheck = true;
        Class hostClass = host.getClass();
        this.preferences = Preferences.userNodeForPackage(hostClass);
    }

    public Host getHost() {
        return host;
    }

    public UpdateDelegate getDelegate() {
        return delegate;
    }

    public void setDelegate(UpdateDelegate delegate) {
        this.delegate = delegate;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    /**
     * Notification sent from the application that the updater can do its thing.
     */
    public void applicationDidFinishLaunching() {
        checkForUpdatesInBackground();
    }

    public boolean isAutomaticCheck() {
        return automaticCheck;
    }

    /**
     * This method is initialized by a user action.
     * <p/>
     * It will check for updates and reports its findings in a user dialog.
     */
    public void checkForUpdates() {
        checkForUpdates(true);
    }

    /**
     * This method is initialized by a user action.
     * <p/>
     * It will check for updates and reports its findings in a user dialog.
     *
     * @param showProgressWindow if true, shows the progress window.
     */
    public void checkForUpdates(boolean showProgressWindow) {
        if (updateChecker != null && updateChecker.isAlive()) return;
        if (showProgressWindow) {
            updateCheckDialog = new UpdateCheckDialog(null, this);
            Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            SwingUtils.centerOnScreen(updateCheckDialog, win);
            updateCheckDialog.setVisible(true);
        }
        updateChecker = new UpdateChecker(this);
        updateChecker.start();
    }

    /**
     * This method gets run when automatically checking for updates.
     * <p/>
     * If no updates are found, results are silently discarded. If an update is found, the update dialog will show.
     */
    public void checkForUpdatesInBackground() {
        if (shouldCheckForUpdate()) {
            updateChecker = new UpdateChecker(this);
            updateChecker.start();
        }
    }

    /**
     * Determines if the last check was far enough in the past to justify a new check.
     *
     * @return if the enough time has passed since the last check.
     */
    public boolean shouldCheckForUpdate() {
        long lastTime = getPreferences().getLong(LAST_UPDATE_CHECK, 0);
        long deltaTime = System.currentTimeMillis() - lastTime;
        return deltaTime > UPDATE_INTERVAL;
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public void checkPerformed(Appcast appcast) {
        if (updateCheckDialog != null) {
            updateCheckDialog.setVisible(false);
            updateCheckDialog = null;
        }
        // Delegate method.
        if (delegate != null)
            if (delegate.checkPerformed(appcast))
                return;

        // Store last check update check time.
        getPreferences().putLong(LAST_UPDATE_CHECK, System.currentTimeMillis());
        try {
            getPreferences().flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException("Error while storing preferences", e);
        }
    }

    public void checkerFoundValidUpdate(Appcast appcast) {
        // Delegate method.
        if (delegate != null)
            if (delegate.checkerFoundValidUpdate(appcast))
                return;

        UpdateAlert alert = new UpdateAlert(this, appcast);
        Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        SwingUtils.centerOnScreen(alert, win);
        alert.setVisible(true);
        alert.downloadReleaseNotes();
    }

    public void checkerEncounteredError(Exception e) {
        if (updateCheckDialog != null) {
            updateCheckDialog.setVisible(false);
            updateCheckDialog = null;
        }
        // Delegate method.
        if (delegate != null)
            if (delegate.checkerEncounteredError(e))
                return;
        throw new RuntimeException(e);
    }

    public void waitForCheck(int timeoutMillis) {
        try {
            updateChecker.join(timeoutMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (updateChecker.isAlive()) {
            updateChecker.interrupt();
        }
    }

    public void cancelUpdateCheck() {
        if (updateChecker != null && updateChecker.isAlive()) {
            updateChecker.interrupt();
        }
    }
}
