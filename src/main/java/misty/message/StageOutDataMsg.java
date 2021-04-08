package misty.message;

public class StageOutDataMsg {
    private final int taskId;
    private final int cycle;
    private final int dstFogDeviceId;
    private final boolean isData;

    public StageOutDataMsg(int taskId, int cycle, int dstFogDeviceId, boolean isData) {
        this.taskId = taskId;
        this.cycle = cycle;
        this.dstFogDeviceId = dstFogDeviceId;
        this.isData = isData;
    }

    public int getTaskId() {
        return taskId;
    }

    public int getDstFogDeviceId() {
        return dstFogDeviceId;
    }

    public int getCycle() {
        return cycle;
    }

    public boolean isData() {
        return isData;
    }
}
