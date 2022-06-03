package widesim.message;

import widesim.computation.Task;

public class TaskIsDoneMsg {
    private final Task task;

    public TaskIsDoneMsg(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}
