package nodebox.node;

import com.google.common.base.Objects;

public class MenuItem {
    
    private final String key;
    private final String label;


    public MenuItem(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MenuItem)) return false;
        final MenuItem other = (MenuItem) o;
        return Objects.equal(key, other.key)
                && Objects.equal(label, other.label);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key, label);
    }
    
    @Override
    public String toString() {
        return String.format("<MenuItem %s (%s)>", label, key);
    }

}
