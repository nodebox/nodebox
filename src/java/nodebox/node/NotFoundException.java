package nodebox.node;

public class NotFoundException extends RuntimeException {

    private Object source;
    private String name;

    public NotFoundException(Object source, String name, String message) {
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
        return super.toString() + "/" + source + "/" + name;
    }
}
