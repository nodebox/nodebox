package nodebox.node;

import java.util.Locale;

public class InvalidNameException extends RuntimeException {

    private Object source;
    private String name;

    public InvalidNameException(Object source, String name, String message) {
        super(message);
        this.source = source;
        this.name = name;
    }

    public Object getSource() {
        return source;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "InvalidNameException on %s name %s: %s", source, name, getMessage());
    }
}
