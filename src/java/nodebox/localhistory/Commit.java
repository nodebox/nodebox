package nodebox.localhistory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Commit {

    public final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Repository repository;
    private String id;
    //private Tree tree;
    private String treeId;
    private String parentId;
    private Commit parent;
    private Date time;
    private String message;

    public Commit(Repository repository, String id) {
        this.repository = repository;
        this.id = id;
        // Retrieve and parse the commit from the object database.
        String commitData = new String(repository.readObject(id));
        // The commit consists of a number of lines, each with identifiers.
        String[] commitLines = commitData.split("\n");
        int pos = 0;
        while (true) {
            // Get a line from the commit data.
            String commitLine = commitLines[pos];
            // Examine the contents to see what it means
            if (commitLine.trim().length() == 0) {
                // Last line of the commit metadata. The rest of the commit is the message.
                break;
            } else if (commitLine.startsWith("tree")) {
                // The tree line contains a reference to the root tree of the commit.
                // The line looks like this (without the quotes): "tree 19e8ac9f8e2349e8ac9f8e2349e8ac9f8e234da1"
                // The 6th character (5, zero-based) is the start of the id.
                // The id is always 40 characters long.
                treeId = commitLine.substring(5);
                if (treeId.length() != 40)
                    throw new AssertionError("Could not parse commit " + id + ": the tree line is in the wrong format. " + commitLine);
            } else if (commitLine.startsWith("parent")) {
                // The parent line contains a reference to the parent commit.
                // The line looks like this (without the quotes): "parent 19e8ac9f8e2349e8ac9f8e2349e8ac9f8e234da1"
                // The 8th character (7, zero-based) is the start of the id.
                // The id is always 40 characters long.
                parentId = commitLine.substring(7);
                if (parentId.length() != 40)
                    throw new AssertionError("Could not parse commit " + id + ": the parent line is in the wrong format. " + commitLine);
            } else if (commitLine.startsWith("time")) {
                // The time line stores when the commit was made.
                // The line looks like this (without the quotes): "time 2009-02-19 18:38:17"
                // The 6th character (5, zero-based) is the start of the time string.
                String timeString = commitLine.substring(5);
                try {
                    time = dateFormat.parse(timeString);
                } catch (ParseException e) {
                    throw new AssertionError("Could not parse commit " + id + ": the time is in the wrong format. (" + e.getMessage() + ") " + commitLine);
                }
            } else {
                throw new AssertionError("Could not parse commit " + id + ": unknown line " + commitLine);
            }
            // Increase the position (move to the next line)
            pos++;
        }
        // Check if the required metadata was parsed (tree and time are required, parent is optional)
        if (treeId == null)
            throw new AssertionError("Could not parse commit" + id + ": no tree encountered in the commit data");
        if (time == null)
            throw new AssertionError("Could not parse commit" + id + ": no time encountered in the commit data");
        StringBuffer messageBuffer = new StringBuffer();
        for (; pos < commitLines.length; pos++)
            messageBuffer.append(commitLines[pos]).append("\n");
        message = messageBuffer.toString();
    }

    public Repository getRepository() {
        return repository;
    }

    public String getId() {
        return id;
    }

    public String getTreeId() {
        return treeId;
    }

    public String getParentId() {
        return parentId;
    }

    public Commit getParent() {
        return parent;
    }

    public Date getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Commit)) return false;
        return id.equals(((Commit) obj).id);
    }
}
