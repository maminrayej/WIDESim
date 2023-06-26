package widesim.entity;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import widesim.computation.Task;
import widesim.core.Constants;
import widesim.core.Logger;
import widesim.failure.FailureGenerator;
import widesim.message.IncomingTaskMsg;
import widesim.message.TaskIsDoneMsg;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class WorkflowEngine extends SimEntity {
    private final int brokerId;
    private final HashMap<Integer, Task> tasks;
    private final HashSet<Integer> waitingTasks;
    private final HashSet<Integer> completedTasks;

    public WorkflowEngine(int brokerId) {
        super("WorkflowEngine");

        tasks = new HashMap<>();
        completedTasks = new HashSet<>();
        waitingTasks = new HashSet<>();
        this.brokerId = brokerId;
    }

    @Override
    public void startEntity() {
        log("Starting WorkflowEngine...");
    }

    @Override
    public void shutdownEntity() {
        log("Shutting down entity...");
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case Constants.MsgTag.INCOMING_TASK -> processIncomingTask(ev);
            case Constants.MsgTag.TASK_IS_DONE -> processCompletedTask(ev);
        }
    }

    protected void processIncomingTask(SimEvent ev) {
        IncomingTaskMsg taskMsg = (IncomingTaskMsg) ev.getData();
        Task task = taskMsg.getTask();

        log("Task(%s) of workflow(%s) received", task.getTaskId(), task.getWorkflowId());


        tasks.put(task.getTaskId(), task);

        if (completedTasks.containsAll(task.getParents())) {
            log("Sending Task(%s) from workflow(%s)...", task.getTaskId(), task.getWorkflowId());
            sendNow(brokerId, Constants.MsgTag.INCOMING_TASK, new IncomingTaskMsg(task));
        } else {
            log("Task(%s) is not ready to get executed. Adding it to waiting queue...", task.getTaskId());
            waitingTasks.add(task.getTaskId());
        }
    }

    protected void processCompletedTask(SimEvent ev) {
        TaskIsDoneMsg doneMsg = (TaskIsDoneMsg) ev.getData();
        Task task = doneMsg.getTask();

        log("Task(%s) received as complete", task.getTaskId());

        if (task.getStatus() == Cloudlet.FAILED) {
            log("Task(%s) failed. Adding it to waiting queue...", task.getTaskId());
            task.addFailedExecution(CloudSim.clock());
            try {
                task.setCloudletStatus(Cloudlet.CREATED);
                task.setExecStartTime(-1);
            } catch (Exception e) {
                log("Error while setting task status to READY");
            }
//            waitingTasks.add(task.getTaskId());
            schedule(this.getId(), 0, Constants.MsgTag.INCOMING_TASK, new IncomingTaskMsg(task));
        } else {
            completedTasks.add(task.getTaskId());
        }

        for (Iterator<Integer> iterator = waitingTasks.iterator(); iterator.hasNext(); ) {
            Integer waitingTaskId = iterator.next();
            Task waitingTask = tasks.get(waitingTaskId);

            if (completedTasks.containsAll(waitingTask.getParents())) {
                iterator.remove();
                log("Task(%s) is now ready to get executed. Releasing it...", waitingTask.getTaskId());
                sendNow(brokerId, Constants.MsgTag.INCOMING_TASK, new IncomingTaskMsg(waitingTask));
            }
        }
    }

    private void log(String formatted, Object... args) {
        String tag = String.format("WorkflowEngine(%s)", getId());

        Logger.log(tag, formatted, args);
    }
}
