package nodebox.graphics;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import nodebox.util.IOrderedFields;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static nodebox.util.ReflectionUtils.getGetterMethod;

/**
 * A record is an abstract data structure that contains associative data based on the input fields.
 */
public class AbstractRecord implements Map<String, Object>, IOrderedFields {

    private final ImmutableList<String> fields;
    private ImmutableMap<String, Method> getterMap;

    protected AbstractRecord(String... fields) {
        this.fields = ImmutableList.copyOf(fields);
    }

    public Iterable<String> getOrderedFields() {
        return fields;
    }

    public int size() {
        return fields.size();
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    public boolean containsKey(Object key) {
        return fields.contains(key);
    }

    public boolean containsValue(Object value) {
        for (String field : fields) {
            Object fieldValue = get(field);
            if (Objects.equal(fieldValue, value)) return true;
        }
        return false;
    }

    private void ensureGetterMap() {
        if (getterMap != null) return;
        ImmutableMap.Builder<String, Method> b = ImmutableMap.builder();
        for (String field : fields) {
            Method m = getGetterMethod(getClass(), field);
            if (m == null) {
                throw new RuntimeException("The field " + field + " could not be found in " + this);
            }
            b.put(field, m);
        }
        getterMap = b.build();
    }

    public Object get(Object key) {
        ensureGetterMap();
        checkArgument(getterMap.containsKey(key), "Key %s not found in object %s", key, this);
        Method getterMethod = getterMap.get(key);
        try {
            return getterMethod.invoke(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object put(String key, Object value) {
        throw new UnsupportedOperationException("Records are immutable.");
    }

    public Object remove(Object key) {
        throw new UnsupportedOperationException("Records are immutable.");
    }

    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException("Records are immutable.");
    }

    public void clear() {
        throw new UnsupportedOperationException("Records are immutable.");
    }

    public Set<String> keySet() {
        return Sets.newHashSet(fields);
    }

    public Collection<Object> values() {
        throw new UnsupportedOperationException("The values() method is unsupported.");
    }

    public Set<Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException("The entrySet() method is unsupported.");
    }

}
