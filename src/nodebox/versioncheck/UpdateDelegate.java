package nodebox.versioncheck;

/**
 * Contains dummy methods that can be overridden to hook into the update checker.
 * Each method returns a boolean to indicate if the method was handled by the delegate.
 * If false, further processing occurs in the caller.
 */
public class UpdateDelegate {

    public boolean checkCompleted(UpdateChecker checker, Appcast appcast) {
        return false;
    }

    public boolean checkerFoundValidUpdate(UpdateChecker checker, Appcast appcast) {
        return false;
    }

    public boolean checkerDetectedLatestVersion(UpdateChecker checker, Appcast appcast) {
        return false;
    }

    public boolean checkerEncounteredError(UpdateChecker checker, Throwable t) {
        return false;
    }
}
