package misty.message;

import misty.computation.Task;

public class IncomingTaskMsg {
    private final Task task;

    public IncomingTaskMsg(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}
