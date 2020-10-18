package misty.entity;

import misty.computation.Task;
import misty.computation.Workflow;
import misty.core.Constants;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TaskManager extends SimEntity {

    private final List<Task> tasks = new ArrayList<>();
    private final Integer brokerId;

    public TaskManager(Integer brokerId, List<Workflow> workflows) {
        super("TaskManager");

        this.brokerId = brokerId;

        for (Workflow workflow : workflows)
            tasks.addAll(workflow.getTasks());

        tasks.sort(Comparator.comparingDouble(Task::getEntryTime));
    }

    @Override
    public void startEntity() {
        System.out.println("Starting Task Manager...");

        // dispatch tasks to broker
        for (Task task : tasks) {
            System.out.printf("TaskManager: Sending task: %s of workflow: %s\n", task.getTaskId(), task.getWorkflowId());
            schedule(brokerId, task.getEntryTime() - CloudSim.clock(), Constants.MsgTag.INCOMING_TASK, task);
        }
    }

    @Override
    public void processEvent(SimEvent ev) {
    }

    @Override
    public void shutdownEntity() {
    }
}
