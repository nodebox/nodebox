package nodebox.versioncheck;

/**
 * Contains dummy methods that can be overridden to hook into the update checker.
 * Each method returns a boolean to indicate if the method was handled by the delegate.
 * If false, further processing occurs in the caller.
 */
public class UpdateDelegate {

    public boolean checkPerformed(Appcast appcast) {
        return false;
    }

    public boolean checkerFoundValidUpdate(Appcast appcast) {
        return false;
    }

    public boolean checkerEncounteredError(Exception e) {
        return false;
    }


}
