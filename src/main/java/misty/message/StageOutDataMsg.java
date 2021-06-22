package misty.message;

import java.util.List;

public class StageOutDataMsg {
    private final int taskId;
    private final List<String> neededFiles;
    private final int cycle;
    private final int dstFogDeviceId;
    private final boolean isData;

    public StageOutDataMsg(int taskId, int cycle, int dstFogDeviceId, boolean isData, List<String> neededFiles) {
        this.taskId = taskId;
        this.cycle = cycle;
        this.dstFogDeviceId = dstFogDeviceId;
        this.isData = isData;
        this.neededFiles = neededFiles;
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

    public List<String> getNeededFiles() {
        return neededFiles;
    }
}
