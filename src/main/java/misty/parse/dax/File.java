package misty.parse.dax;

public class File {
    private final String id;
    private final long size;

    public File(String id, long size) {
        this.id = id;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public long getSize() {
        return size;
    }
}
