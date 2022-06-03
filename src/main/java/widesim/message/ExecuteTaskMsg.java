package widesim.message;

import widesim.computation.Task;

public class ExecuteTaskMsg {
    private final Task task;
    private final int vmId;

    public ExecuteTaskMsg(Task task, int vmId) {
        this.task = task;
        this.vmId = vmId;
    }

    public Task getTask() {
        return task;
    }

    public int getVmId() {
        return vmId;
    }
}
