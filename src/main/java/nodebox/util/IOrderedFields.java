package nodebox.util;

/**
 * An interface that defines a list of ordered fields.
 * 
 * This is implemented by AbstractRecord, and used for the data sheet where we can have a preferred ordering.
 */
public interface IOrderedFields {

    public Iterable<String> getOrderedFields();

}
