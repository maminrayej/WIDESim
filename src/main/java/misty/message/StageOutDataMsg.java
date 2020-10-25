package misty.message;

public class StageOutDataMsg {
    private final int taskId;
    private final int dstFogDeviceId;

    public StageOutDataMsg(int taskId, int dstFogDeviceId) {
        this.taskId = taskId;
        this.dstFogDeviceId = dstFogDeviceId;
    }

    public int getTaskId() {
        return taskId;
    }

    public int getDstFogDeviceId() {
        return dstFogDeviceId;
    }
}
