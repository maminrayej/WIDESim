package widesim.computation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Workflow {
    private final List<Task> tasks;
    private final Map<Integer, Task> taskMap;
    private final String workflowId;

    public Workflow(List<Task> tasks, String workflowId) {
        this.tasks = tasks;
        taskMap = tasks.stream().collect(Collectors.toMap(Task::getTaskId, task -> task));
        this.workflowId = workflowId;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public Task getTask(int taskId) {
        return taskMap.getOrDefault(taskId, null);
    }
}
