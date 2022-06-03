package widesim.entity;

import widesim.computation.Task;
import widesim.computation.Workflow;
import widesim.core.Constants;
import widesim.core.Logger;
import widesim.message.IncomingTaskMsg;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TaskManager extends SimEntity {

    private final List<Task> tasks = new ArrayList<>();
    private final Integer workflowEngineId;

    public TaskManager(Integer workflowEngineId, List<Workflow> workflows) {
        super("TaskManager");

        this.workflowEngineId = workflowEngineId;

        for (Workflow workflow : workflows)
            tasks.addAll(workflow.getTasks());

        tasks.sort(Comparator.comparingDouble(Task::getEntryTime));
    }

    @Override
    public void startEntity() {
        log("Starting Task Manager...");

        // dispatch tasks to broker
        for (Task task : tasks) {
            log("Sending task: %s of workflow: %s", task.getTaskId(), task.getWorkflowId());
            schedule(workflowEngineId, task.getEntryTime() - CloudSim.clock(), Constants.MsgTag.INCOMING_TASK, new IncomingTaskMsg(task));
        }
    }

    @Override
    public void processEvent(SimEvent ev) {
    }

    @Override
    public void shutdownEntity() {
    }

    private void log(String formatted, Object... args) {
        String tag = String.format("TaskManager(%s)", getId());

        Logger.log(tag, formatted, args);
    }
}
