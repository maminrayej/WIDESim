package widesim.message;

public class BroadcastIdMsg {
    private final String name;
    private final int id;

    public BroadcastIdMsg(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }
}
