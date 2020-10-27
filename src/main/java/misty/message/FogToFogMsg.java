package misty.message;

import misty.computation.Data;

public class FogToFogMsg {
    private final int dstFogDeviceId;
    private final int taskId;
    private final long data;


    public FogToFogMsg(int dstFogDeviceId, int taskId, long data) {
        this.dstFogDeviceId = dstFogDeviceId;
        this.taskId = taskId;
        this.data = data;
    }

    public int getDstFogDeviceId() {
        return dstFogDeviceId;
    }

    public int getTaskId() {
        return taskId;
    }

    public long getData() {
        return data;
    }
}
