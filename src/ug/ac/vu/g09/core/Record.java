package ug.ac.vu.g09.core;

/**
 * Base class for everything the hostel system stores.
 * @author Mande Roland
 */
public abstract class Record {
    protected String id;
    protected String name;

    public Record(String id, String name) {
        if (id == null || !id.startsWith("G09-")) {
            throw new IllegalArgumentException("ID must start with G09-");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        // the pipe is our separator in the data files
        if (name.contains("|") || id.contains("|")) {
            throw new IllegalArgumentException("The | symbol is not allowed");
        }
        this.id = id;
        this.name = name.trim();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public abstract String describe();

    public abstract String toFileLine();
}
