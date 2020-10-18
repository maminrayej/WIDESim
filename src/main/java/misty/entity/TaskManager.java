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
    private final String brokerId;

    public TaskManager(String brokerId, List<Workflow> workflows) {
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
        for (Task task : tasks)
            schedule(brokerId, task.getEntryTime() - CloudSim.clock(), Constants.MsgTag.TASK_INCOMING, task);
    }

    @Override
    public void processEvent(SimEvent ev) {
    }

    @Override
    public void shutdownEntity() {
    }
}
