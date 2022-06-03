package widesim.message;

import widesim.computation.Task;

public class IncomingTaskMsg {
    private final Task task;

    public IncomingTaskMsg(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}
