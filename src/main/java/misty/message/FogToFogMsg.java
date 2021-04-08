package misty.message;

import misty.computation.Data;

public class FogToFogMsg {
    private final int dstFogDeviceId;
    private final int taskId;
    private final int cycle;
    private final long data;
    private final boolean isData;


    public FogToFogMsg(int dstFogDeviceId, int taskId, int cycle, long data, boolean isData) {
        this.dstFogDeviceId = dstFogDeviceId;
        this.taskId = taskId;
        this.cycle = cycle;
        this.data = data;
        this.isData = isData;
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

    public int getCycle() {
        return cycle;
    }

    public boolean isData() {
        return isData;
    }
}
