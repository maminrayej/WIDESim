package misty.computation;

import java.util.List;

public class Workflow {
    private final List<Task> tasks;

    public Workflow(List<Task> tasks) {
        this.tasks = tasks;
    }

    public List<Task> getTasks() {
        return tasks;
    }
}
