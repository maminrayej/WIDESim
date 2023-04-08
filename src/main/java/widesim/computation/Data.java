package widesim.computation;

public class Data {

    private final int srcTaskId;
    private final int dstTaskId;
    private final double size;
    private final String fileName;

    public Data(String fileName, int srcTaskId, int dstTaskId, double size) {
        this.fileName = fileName;
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

    public double getSize() {
        return size;
    }

    public String getFileName() {
        return fileName;
    }
}
