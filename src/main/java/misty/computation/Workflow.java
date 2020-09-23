package misty.computation;

import java.util.List;

public class Workflow {
    private final List<Task> tasks;
    private final String workflowId;

    public Workflow(List<Task> tasks, String workflowId) {
        this.tasks = tasks;
        this.workflowId = workflowId;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public String getWorkflowId() {
        return workflowId;
    }
}
