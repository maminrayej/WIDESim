package widesim.entity;

import widesim.computation.Task;
import widesim.core.Constants;
import widesim.core.Logger;
import widesim.failure.FailureGenerator;
import widesim.mapper.TaskToVmMapper;
import widesim.mapper.VmToFogDeviceMapper;
import widesim.message.*;
import widesim.provision.VmProvisioner;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;
import org.jgrapht.alg.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class FogBroker extends PowerDatacenterBroker {

    private int workflowEngineId;

    private final List<Integer> fogDeviceIds;
    private final List<FogDevice> fogDevices;
    private final Map<Integer, DatacenterCharacteristics> fogDeviceIdToCharacteristics;

    private final long downLinkBw;
    private final long upLinkBw;

    private final HashMap<Pair<Integer, Integer>, Integer> routingTable;

    // variables for vm management
    private final VmProvisioner vmProvisioner;
    private final VmToFogDeviceMapper vmToFogDeviceMapper;
    private final Set<Integer> sentVmCreateRequests;
    private final Set<Integer> vmCreateAcks;
    private final Set<Integer> sentVmDestroyRequests;
    private final Set<Integer> vmDestroyAcks;
    private final List<PowerVm> failedVms;
    // variables for task management
    private final Map<Integer, Task> tasks;
    private final List<Task> waitingTaskQueue;
    private final TaskToVmMapper taskToVmMapper;
    private final Set<Task> dispatchedTasks;
    private final Set<Task> completedTasks;

    private Map<Integer, Integer> vmToFogDevice;
    private List<PowerVm> createdVms;
    private List<Integer> toBeCreated;
    private final Map<Integer, Integer> taskToVm;

    private final int maximumCycle = 0;

    public FogBroker(String name, VmProvisioner vmProvisioner, VmToFogDeviceMapper vmToFogDeviceMapper,
                     TaskToVmMapper taskToVmMapper, long downLinkBw, long upLinkBw,
                     HashMap<Pair<Integer, Integer>, Integer> routingTable, List<FogDevice> fogDevices) throws Exception {
        super(name);

        this.waitingTaskQueue = new ArrayList<>();
        this.fogDeviceIds = new ArrayList<>();
        this.fogDevices = fogDevices;
        this.fogDeviceIdToCharacteristics = new HashMap<>();

        this.vmProvisioner = vmProvisioner;
        this.vmToFogDeviceMapper = vmToFogDeviceMapper;
        this.sentVmCreateRequests = new HashSet<>();
        this.vmCreateAcks = new HashSet<>();
        this.sentVmDestroyRequests = new HashSet<>();
        this.vmDestroyAcks = new HashSet<>();
        this.createdVms = new ArrayList<>();
        this.failedVms = new ArrayList<>();
        this.toBeCreated = new ArrayList<>();

        this.tasks = new HashMap<>();
        this.taskToVmMapper = taskToVmMapper;
        this.taskToVm = new HashMap<>();
        this.vmToFogDevice = new HashMap<>();
        this.dispatchedTasks = new HashSet<>();
        this.completedTasks = new HashSet<>();

        this.upLinkBw = upLinkBw;
        this.downLinkBw = downLinkBw;

        this.routingTable = routingTable;
    }

    @Override
    public void startEntity() {
        // log("Starting Broker");
        log("Starting FogBroker...");

        // broker sends INIT message to itself to start
        schedule(getId(), 0, Constants.MsgTag.INIT);
    }

    @Override
    public void processEvent(SimEvent event) {
        switch (event.getTag()) {
            case Constants.MsgTag.INIT -> init();
            case Constants.MsgTag.INCOMING_TASK -> processIncomingTask(event);
            case Constants.MsgTag.RESOURCE_REQUEST_RESPONSE -> processResourceRequestResponse(event);
            case Constants.MsgTag.VM_CREATE_ACK -> processVmCreateAck(event);
            case Constants.MsgTag.VM_DESTROY_ACK -> processVmDestroyAck(event);
            case CloudSimTags.CLOUDLET_SUBMIT_ACK -> processTaskAck(event);
            case CloudSimTags.CLOUDLET_RETURN -> processTaskIsDone(event);
            default -> super.processEvent(event);
        }
    }

    @Override
    public void shutdownEntity() {

    }

    protected void init() {
        log("Requesting for fog devices characteristics...");

        this.fogDeviceIds.addAll(CloudSim.getCloudResourceList());

        for (Integer fogDeviceId : this.fogDeviceIds) {
            sendNow(fogDeviceId, Constants.MsgTag.RESOURCE_REQUEST);
        }
    }

    protected void processIncomingTask(SimEvent event) {
        IncomingTaskMsg incomingTaskMsg = (IncomingTaskMsg) event.getData();
        Task task = incomingTaskMsg.getTask();

        task.setUserId(getId());
        task.getTaskState().setEnterBrokerWaitingQueue(task.getCycle(), CloudSim.clock());
        this.waitingTaskQueue.add(task);
        this.tasks.put(task.getTaskId(), task);

        log("Task(%s) of Workflow(%s) received", task.getTaskId(), task.getWorkflowId());

        // Wait for vms to get created, Then dispatch each incoming task
        // Otherwise tasks will be dispatched after every vm is created(or failed to be created)
        if (this.vmCreateAcks.containsAll(this.sentVmCreateRequests))
            dispatchTasks();
    }

    protected void processResourceRequestResponse(SimEvent event) {
        ResourceRequestResponseMsg responseMsg = (ResourceRequestResponseMsg) event.getData();

        this.fogDeviceIdToCharacteristics.put(event.getSource(), responseMsg.getCharacteristics());

        // if all fog devices have sent their characteristics, try to create vms
        if (this.fogDeviceIdToCharacteristics.size() == this.fogDeviceIds.size()) {
            log("All resources received");

            this.vmToFogDevice = this.vmToFogDeviceMapper.map(this.fogDeviceIdToCharacteristics, this.getVmList(), fogDevices);

            for (Integer vmId : this.vmToFogDevice.keySet()) {
                var fogDeviceId = this.vmToFogDevice.get(vmId);

                sendNow(
                        fogDeviceId, // send message to fog device
                        Constants.MsgTag.VM_CREATE, // tell fog device to create a vm
                        new VmCreateMsg(VmList.getById(this.getVmList(), vmId)) // vm
                );

                this.sentVmCreateRequests.add(vmId);
            }
        }
    }

    protected void processVmCreateAck(SimEvent event) {
        VmCreateAckMsg ackMsg = (VmCreateAckMsg) event.getData();
        int vmId = ackMsg.getVmId();
        boolean isCreated = ackMsg.isCreated;

        this.vmCreateAcks.add(vmId);

        if (isCreated)
            this.createdVms.add(VmList.getById(this.getVmList(), vmId));
        else
            this.failedVms.add(VmList.getById(this.getVmList(), vmId));

        // if all fog devices responded to vm creation requests, dispatch cloudlets
        if (this.vmCreateAcks.containsAll(this.sentVmCreateRequests)) {
            log("All pending vm create acks received");

            dispatchTasks();
        }
    }

    protected void processTaskAck(SimEvent event) {
        int[] data = (int[]) event.getData();
        int taskId = data[1];
        boolean isSubmitted = data[2] == CloudSimTags.TRUE;

        log("Task(%s) ack: %s", taskId, isSubmitted);
    }

    protected void processVmDestroyAck(SimEvent event) {
        this.vmDestroyAcks.add(event.getSource());

        if (this.vmDestroyAcks.containsAll(this.sentVmDestroyRequests)) {
            log("All vm destroy acks received");

            this.sentVmCreateRequests.clear();
            this.vmCreateAcks.clear();
            for (int vmId : this.toBeCreated) {
                var fogDeviceId = this.vmToFogDevice.get(vmId);

                sendNow(
                        this.vmToFogDevice.get(vmId), // fog device containing the vm
                        Constants.MsgTag.VM_CREATE, // tell the fog device to create the vm
                        new VmCreateMsg(VmList.getById(getVmList(), vmId))
                );
                this.sentVmCreateRequests.add(fogDeviceId);
            }
        }
    }

    protected void processTaskIsDone(SimEvent event) {
        Task task = (Task) event.getData();
        log("Received task completion msg for Task(%s) from FogDevice(%s)", task.getTaskId(), event.getSource());


        task.getTaskState().setEndExecutionTime(task.getCycle(), CloudSim.clock());
        boolean failed = FailureGenerator.generate(task);
        log("Notifying WorkflowEngine of completed Task(%s)...", task.getTaskId());
        sendNow(workflowEngineId, Constants.MsgTag.TASK_IS_DONE, new TaskIsDoneMsg(task));

        task = task.getNextCycle();

        if (!failed) {

            this.completedTasks.add(task);
            this.tasks.put(task.getTaskId(), task);

            task.getTaskState().setEnterBrokerWaitingQueue(task.getCycle(), CloudSim.clock());

            // Stage out data of the task to its dispatched children
            int taskId = task.getTaskId();
            boolean isData = task.wantToGenerateData(task.getCycle() - 1, CloudSim.clock());
            log("Task(%s) on Cycle(%s) decided to generate data: %s", task.getTaskId(), task.getCycle() - 1, isData);

            // Ask fog device hosting the task to send output of the task to its child tasks.
            for (Task child : dispatchedTasks.stream().filter(t -> t.getParents().contains(taskId)).collect(Collectors.toList())) {
                int vmId = taskToVm.get(child.getTaskId());
                int fogDeviceId = vmToFogDevice.get(vmId);

                List<String> fileNames = child.neededFrom(task.getTaskId());
                log("Sending STAGE_OUT_MSG to FogDevice(%s) for Task(%s) on Cycle(%s): (%s)", event.getSource(), task.getTaskId(), task.getCycle() - 1, isData);
                sendNow(
                        event.getSource(),
                        Constants.MsgTag.STAGE_OUT_DATA,
                        new StageOutDataMsg(task.getTaskId(), task.getCycle() - 1, fogDeviceId, isData, fileNames)
                );
            }

            if (task.getCycle() <= maximumCycle) {
                // Ask fog devices hosting parent tasks to send their output to fog device hosting this task
                for (int parentId : task.getParents()) {
                    int fogDeviceId = this.vmToFogDevice.get(this.taskToVm.get(parentId));
                    Task parentTask = tasks.get(parentId);
                    List<String> neededFiles = task.neededFrom(parentId);
                    if (parentTask.didYouGenerateData(task.getCycle()) != null) {
                        log("Sending STAGE_OUT_MSG to FogDevice(%s) for Task(%s) on Cycle(%s): (%s)", fogDeviceId, parentId, task.getCycle(), parentTask.didYouGenerateData(task.getCycle()));
                        sendNow(
                                fogDeviceId,
                                Constants.MsgTag.STAGE_OUT_DATA,
                                new StageOutDataMsg(parentId, task.getCycle(), event.getSource(), parentTask.didYouGenerateData(task.getCycle()), neededFiles)
                        );
                    } else {
                        log("Parent Task(%s) has not reached Cycle(%s) yet", parentId, task.getCycle());
                    }

                }

                // Ask the fog device to execute the task again on the new cycle
                if (!task.getParents().isEmpty()) {
                    log("Asking FogDevice(%s) to execute Task(%s) on Cycle(%s)", event.getSource(), task.getTaskId(), task.getCycle());
                    task.getTaskState().setExitBrokerWaitingQueue(task.getCycle(), CloudSim.clock());
                    sendNow(
                            event.getSource(),
                            Constants.MsgTag.EXECUTE_TASK,
                            new ExecuteTaskMsg(task, task.getVmId())
                    );
                } else {
                    log("Asking FogDevice(%s) to execute root Task(%s) on Cycle(%s)", event.getSource(), task.getTaskId(), task.getCycle());
                    double nextExecutionTime = task.getNextTimeExecution(CloudSim.clock());
                    task.getTaskState().setExitBrokerWaitingQueue(task.getCycle(), nextExecutionTime);
                    send(
                            event.getSource(),
                            task.getNextTimeExecution(CloudSim.clock()) - CloudSim.clock(),
                            Constants.MsgTag.EXECUTE_TASK,
                            new ExecuteTaskMsg(task, task.getVmId())
                    );
                }
            }

            // If all dispatched tasks are complete
            if (this.dispatchedTasks.size() == this.completedTasks.size()) {
                log("All dispatched tasks are complete");

                var triple = this.vmProvisioner.provision(
                        this.failedVms,
                        this.createdVms,
                        this.getVmList(),
                        this.taskToVm,
                        this.completedTasks,
                        this.dispatchedTasks,
                        this.waitingTaskQueue
                );

                this.toBeCreated = triple.getFirst(); // to be created vms
                var toBeDestroyed = triple.getSecond(); // to be destroyed vms
                var stayAlive = triple.getThird(); // already created vms

                // Destroy vms
                this.sentVmDestroyRequests.clear();
                this.vmDestroyAcks.clear();
                for (int vmId : toBeDestroyed) {
                    var fogDeviceId = this.vmToFogDevice.get(vmId);

                    sendNow(
                            fogDeviceId, // fog device containing the vm
                            Constants.MsgTag.VM_DESTROY, // tell the fog device to destroy the vm
                            new VmDestroyMsg(vmId)
                    );
                    this.sentVmDestroyRequests.add(fogDeviceId);
                }

                if (this.sentVmDestroyRequests.isEmpty()) {
                    log("There is no vm to destroy");

                    // Create vms
                    this.sentVmCreateRequests.clear();
                    this.vmCreateAcks.clear();
                    for (int vmId : this.toBeCreated) {
                        var fogDeviceId = this.vmToFogDevice.get(vmId);

                        sendNow(
                                this.vmToFogDevice.get(vmId), // fog device containing the vm
                                Constants.MsgTag.VM_CREATE, // tell the fog device to create the vm
                                new VmCreateMsg(VmList.getById(getVmList(), vmId))
                        );
                        this.sentVmCreateRequests.add(fogDeviceId);
                    }
                }

                this.createdVms.clear();
                this.failedVms.clear();

                // Add stayAlive vms to createdVms because they are already created
                this.createdVms = stayAlive.stream().map(vmId -> (PowerVm) VmList.getById(this.getVmList(), vmId)).collect(Collectors.toList());

                // If there is no vm to create either just execute the tasks
                if (this.sentVmCreateRequests.isEmpty()) {
                    log("There is no vm to create");
                    log("Trying to dispatch tasks...");
                    dispatchTasks();
                }
            }
        }
    }

    private void dispatchTasks() {
        log("Dispatching tasks...");
        // map each task in task queue to a vm(either a created or failed one)
        var newTaskToVm = this.taskToVmMapper.map(
                this.createdVms,
                this.failedVms,
                this.waitingTaskQueue,
                this.completedTasks,
                this.dispatchedTasks,
                this.taskToVm,
                routingTable,
                vmToFogDevice
        );

        // merge new mapping with the old one
        this.taskToVm.putAll(newTaskToVm);

        for (int taskId : this.taskToVm.keySet()) {
            this.tasks.get(taskId).setVmId(this.taskToVm.get(taskId));
        }

        for (Iterator<Task> it = this.waitingTaskQueue.iterator(); it.hasNext(); ) {
            Task task = it.next();

            // vm for current task
            Integer mappedVmId = this.taskToVm.get(task.getTaskId());

            // if vm is successfully created, and all parent tasks are complete,
            // dispatch the task the to fog device which contains the vm
            if (this.createdVms.stream().anyMatch(vm -> vm.getId() == mappedVmId)) {
                int dstFogDeviceId = this.vmToFogDevice.get(mappedVmId);

                // inform fog devices containing parent tasks to send output of parent tasks to the fog device containing the child task
                for (int parentId : task.getParents()) {
                    int fogDeviceId = this.vmToFogDevice.get(this.taskToVm.get(parentId));
                    Task parentTask = tasks.get(parentId);
                    log("Sending STAGE_OUT_MSG to FogDevice(%s) for Task(%s) on Cycle(%s)", fogDeviceId, parentId, task.getCycle());
                    List<String> neededFiles = task.neededFrom(parentId);
                    sendNow(
                            fogDeviceId,
                            Constants.MsgTag.STAGE_OUT_DATA,
                            new StageOutDataMsg(parentId, task.getCycle(), dstFogDeviceId, parentTask.didYouGenerateData(task.getCycle()), neededFiles)
                    );
                }

                // if task does not have any parent, its data must be stage in
                if (task.isRoot()) {
//                    double delay = (double) task.getTotalInputDataSize() / this.upLinkBw;
                    double delay = 0.11;
                    log("Sending STAGE_IN data for Task(%s) to FogDevice(%s) with delay: %.2f", task.getTaskId(), dstFogDeviceId, delay);
                    send(
                            dstFogDeviceId,
                            delay,
                            Constants.MsgTag.EXECUTE_TASK_WITH_DATA,
                            new ExecuteTaskMsg(task, mappedVmId)
                    );
                } else {
                    log("Asking FogDevice(%s) to execute Task(%s) on Cycle(%s)", dstFogDeviceId, task.getTaskId(), task.getCycle());
                    sendNow(
                            dstFogDeviceId, // fog device containing the vm
                            Constants.MsgTag.EXECUTE_TASK, // tell fog device to execute the task
                            new ExecuteTaskMsg(task, mappedVmId) // send task with its corresponding vm
                    );
                }

                this.dispatchedTasks.add(task);

                // remove dispatched task from waiting queue
                task.getTaskState().setExitBrokerWaitingQueue(task.getCycle(), CloudSim.clock());
                it.remove();
            }
        }
    }

    @Override
    public <T extends Cloudlet> List<T> getCloudletReceivedList() {
        return (List<T>) new ArrayList<>(this.completedTasks);
    }

    public void setWorkflowEngineId(int workflowEngineId) {
        this.workflowEngineId = workflowEngineId;
    }

    public List<Task> getReceivedTasks() {
        return new ArrayList<>(this.tasks.values());
    }

    public int getMaximumCycle() {
        return maximumCycle;
    }

    private void log(String formatted, Object... args) {
        String tag = String.format("FogBroker(%s)", getId());

        Logger.log(tag, formatted, args);
    }

    public Map<Integer, Integer> getVmToFogDevice() {
        return vmToFogDevice;
    }


}
