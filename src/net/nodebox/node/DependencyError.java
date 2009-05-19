package net.nodebox.node;

public class DependencyError extends RuntimeException {

    private Parameter dependency;
    private Parameter dependent;

    public DependencyError(Parameter dependency, Parameter dependent, String message) {
        super(message);
        this.dependency = dependency;
        this.dependent = dependent;
    }

    public Parameter getDependency() {
        return dependency;
    }

    public Parameter getDependent() {
        return dependent;
    }
}
