package net.nodebox.node;

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

}
