package nodebox.node;

public class StampExpression {

    private Parameter parameter;
    private String stampKey;
    private Expression expression;

    /**
     * Create a new stamp expression. The stamp expression should be set as the value of the given parameter,
     * in the format "width = CNUM * 5"
     *
     * @param node          the node this expression operates on.
     * @param parameterName the name of the parameter for this expression.
     */
    public StampExpression(Node node, String parameterName) {
        if (!node.hasParameter(parameterName)) {
            throw new IllegalArgumentException("The node \"" + node.getName() + "\" has no parameter \"" + parameterName + "\".");
        }
        parameter = node.getParameter(parameterName);
        String stampExpression = parameter.asString();
        if (stampExpression.trim().length() == 0) return;

        // Split the stamp expression into the key and the actual expression.
        int equalsPos = stampExpression.indexOf('=');
        if (equalsPos < 0) {
            throw new IllegalArgumentException("The stamp expression \"" + stampExpression + "\" is not in the format \"width = CNUM * 5\"");
        }
        stampKey = stampExpression.substring(0, equalsPos);
        String expressionString = stampExpression.substring(equalsPos + 1);

        // Convert the expression string to an Expression object.
        expression = new Expression(parameter, expressionString);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public String getStampKey() {
        return stampKey;
    }

    public Expression getExpression() {
        return expression;
    }

    /**
     * Evaluate the expression and store the result under the stamp expression key.
     *
     * @param context the current processing context
     * @throws ExpressionError if an error occurs in the expression
     */
    public void evaluate(ProcessingContext context) throws ExpressionError {
        if (expression == null) return;
        Object result = expression.evaluate(context);
        context.put(stampKey, result);
    }
}
