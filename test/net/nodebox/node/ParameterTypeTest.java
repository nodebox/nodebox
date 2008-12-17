package net.nodebox.node;

import junit.framework.TestCase;

public class ParameterTypeTest extends TestCase {

    public void testNaming() {
        NodeType nt = new TestManager.Number(null);

        checkInvalidName(nt, "1234", "names cannot start with a digit.");

        checkInvalidName(nt, "node", "names can not be one of the reserved words.");
        checkInvalidName(nt, "root", "names can not be one of the reserved words.");
        checkInvalidName(nt, "network", "names can not be one of the reserved words.");

        checkInvalidName(nt, "UPPERCASE", "names cannot be in uppercase.");
        checkInvalidName(nt, "uPpercase", "names cannot contain uppercase letters");
        checkInvalidName(nt, "__reserved", "names cannot start with double underscores");
        checkInvalidName(nt, "what!", "Only lowercase, numbers and underscore are allowed");
        checkInvalidName(nt, "$-#34", "Only lowercase, numbers and underscore are allowed");
        checkInvalidName(nt, "", "names cannot be empty");
        checkInvalidName(nt, "very_very_very_very_very_very_long_name", "names cannot be longer than 30 characters");

        checkValidName(nt, "radius");
        checkValidName(nt, "_test");
        checkValidName(nt, "_");
        checkValidName(nt, "_1234");
        checkValidName(nt, "a1234");

        checkInvalidName(nt, "radius", "parameter type names must be unique for the node type");
    }

    //// Helper functions ////

    private void checkInvalidName(NodeType nt, String newName, String reason) {
        try {
            nt.addParameterType(newName, ParameterType.Type.INT);
            fail("the following condition was not met: " + reason);
        } catch (InvalidNameException e) {
        }
    }

    private void checkValidName(NodeType nt, String newName) {
        try {
            nt.addParameterType(newName, ParameterType.Type.INT);
        } catch (InvalidNameException e) {
            fail("The name \"" + newName + "\" should have been accepted.");
        }
    }
}
