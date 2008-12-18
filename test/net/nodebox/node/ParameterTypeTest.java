package net.nodebox.node;

import net.nodebox.graphics.Canvas;
import net.nodebox.graphics.Color;
import net.nodebox.graphics.Group;
import net.nodebox.graphics.Image;

public class ParameterTypeTest extends NodeTestCase {

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

    public void testDefaultValue() {
        NodeType customType = numberType.clone();
        ParameterType ptInt = customType.addParameterType("int", ParameterType.Type.INT);
        ParameterType ptFloat = customType.addParameterType("float", ParameterType.Type.FLOAT);
        ParameterType ptString = customType.addParameterType("string", ParameterType.Type.STRING);
        ParameterType ptColor = customType.addParameterType("color", ParameterType.Type.COLOR);
        ParameterType ptCanvas = customType.addParameterType("canvas", ParameterType.Type.GROB_CANVAS);
        ParameterType ptVector = customType.addParameterType("vector", ParameterType.Type.GROB_VECTOR);
        ParameterType ptImage = customType.addParameterType("image", ParameterType.Type.GROB_IMAGE);

        assertEquals(0, ptInt.getDefaultValue());
        assertEquals(0.0, ptFloat.getDefaultValue());
        assertEquals("", ptString.getDefaultValue());
        assertEquals(new Color(), ptColor.getDefaultValue());
        assertEquals(new Canvas(), ptCanvas.getDefaultValue());
        assertEquals(new Group(), ptVector.getDefaultValue());
        assertEquals(new Image(), ptImage.getDefaultValue());
    }

    public void testValidate() {
        NodeType customType = numberType.clone();
        ParameterType ptFloat = customType.addParameterType("float", ParameterType.Type.FLOAT);
        assertInvalidValue(ptFloat, "A");
        assertInvalidValue(ptFloat, new Color());
        assertInvalidValue(ptFloat, new Canvas());
        assertValidValue(ptFloat, 1.0);

        ParameterType ptColor = customType.addParameterType("color", ParameterType.Type.COLOR);
        assertInvalidValue(ptColor, "A");
        assertInvalidValue(ptColor, 2);
        assertValidValue(ptColor, new Color());

        // Toggle has a hard bounded range between 0 and 1.
        ParameterType ptToggle = customType.addParameterType("toggle", ParameterType.Type.TOGGLE);
        assertInvalidValue(ptToggle, "A");
        assertInvalidValue(ptToggle, -1);
        assertInvalidValue(ptToggle, 100);
        assertValidValue(ptToggle, 0);
        assertValidValue(ptToggle, 1);
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

    private void assertValidValue(ParameterType pt, Object value) {
        try {
            pt.validate(value);
        } catch (ValueError e) {
            fail("The value '" + value + "' should have been accepted: " + e);
        }
    }

    private void assertInvalidValue(ParameterType pt, Object value) {
        try {
            pt.validate(value);
            fail("The value '" + value + "' should not have been accepted.");
        } catch (ValueError e) {
        }
    }

}
