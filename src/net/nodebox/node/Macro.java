package net.nodebox.node;

public class Macro extends Network {

    private String description;

    public Macro(Parameter.Type outputType) {
        super(outputType);
    }

    public Macro(Parameter.Type outputType, String name) {
        super(outputType, name);
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
