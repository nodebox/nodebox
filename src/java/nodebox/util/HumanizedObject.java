package nodebox.util;

public class HumanizedObject {
    private Object object;

    public HumanizedObject(Object o) {
        this.object = o;
    }

    public Object getObject() {
        return object;
    }

    @Override
    public String toString() {
        return StringUtils.humanizeConstant(object.toString());
    }
}
