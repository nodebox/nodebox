package nodebox.node;

public class ExpressionError extends Exception {

    public ExpressionError() {
    }

    public ExpressionError(String message) {
        super(message);
    }

    public ExpressionError(String message, Throwable cause) {
        super(message, cause);
    }

    public ExpressionError(Throwable cause) {
        super(cause);
    }
}
