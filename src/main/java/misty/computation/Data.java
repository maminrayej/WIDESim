package misty.computation;

public class Data {

    private final int srcTaskId;
    private final int dstTaskId;
    private final long size;

    public Data(int srcTaskId, int dstTaskId, long size) {
        this.srcTaskId = srcTaskId;
        this.dstTaskId = dstTaskId;
        this.size = size;
    }

    public int getSrcTaskId() {
        return srcTaskId;
    }

    public int getDstTaskId() {
        return dstTaskId;
    }

    public long getSize() {
        return size;
    }
}
