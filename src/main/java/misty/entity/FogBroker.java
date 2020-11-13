package misty.entity;

import misty.computation.Task;
import misty.core.Constants;
import misty.core.Logger;
import misty.mapper.TaskToVmMapper;
import misty.mapper.VmToFogDeviceMapper;
import misty.message.*;
import misty.provision.VmProvisioner;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

import java.util.*;
import java.util.stream.Collectors;

public class FogBroker extends DatacenterBroker {

    private final List<Integer> fogDeviceIds;
    private final Map<Integer, DatacenterCharacteristics> fogDeviceIdToCharacteristics;

    private final long downLinkBw;
    private final long upLinkBw;

    // variables for vm management
    private final VmProvisioner vmProvisioner;
    private final VmToFogDeviceMapper vmToFogDeviceMapper;
    private final Set<Integer> sentVmCreateRequests;
    private final Set<Integer> vmCreateAcks;
    private final Set<Integer> sentVmDestroyRequests;
    private final Set<Integer> vmDestroyAcks;
    private final List<Vm> failedVms;
    // variables for task management
    private final Map<Integer, Task> tasks;
    private final List<Task> waitingTaskQueue;
    private final TaskToVmMapper taskToVmMapper;
    private final Set<Task> dispatchedTasks;
    private final Set<Task> completedTasks;
    private Map<Integer, Integer> vmToFogDevice;
    private List<Vm> createdVms;
    private List<Integer> toBeCreated;
    private final Map<Integer, Integer> taskToVm;

    public FogBroker(String name, VmProvisioner vmProvisioner, VmToFogDeviceMapper vmToFogDeviceMapper, TaskToVmMapper taskToVmMapper, long downLinkBw, long upLinkBw) throws Exception {
        super(name);

        this.waitingTaskQueue = new ArrayList<>();
        this.fogDeviceIds = new ArrayList<>();
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
    }

    @Override
    public void startEntity() {
//        log("Starting Broker");
        System.out.println("Starting FogBroker...");

        // broker sends INIT message to itself to start
        schedule(getId(), 0, Constants.MsgTag.INIT);
    }

    @Override
    public void processEvent(SimEvent event) {

        switch (event.getTag()) {
            // start the broker by requesting each fog device for its characteristics
            case Constants.MsgTag.INIT:
                init();
                break;
            // process incoming task from Task Manager
            case Constants.MsgTag.INCOMING_TASK:
                processIncomingTask(event);
                break;
            case Constants.MsgTag.RESOURCE_REQUEST_RESPONSE:
                processResourceRequestResponse(event);
                break;
            case Constants.MsgTag.VM_CREATE_ACK:
                processVmCreateAck(event);
                break;
            case Constants.MsgTag.VM_DESTROY_ACK:
                processVmDestroyAck(event);
                break;
            case CloudSimTags.CLOUDLET_SUBMIT_ACK:
                processTaskAck(event);
                break;
            case CloudSimTags.CLOUDLET_RETURN:
                processTaskIsDone(event);
                break;
            default:
                super.processEvent(event);
                break;
        }
    }

    @Override
    public void shutdownEntity() {

    }

    protected void init() {
        log("Requesting for fog devices characteristics...");

        // store id of all fog devices that registered their selves
        this.fogDeviceIds.addAll(CloudSim.getCloudResourceList());

        log("Received fog device ids: %s", this.fogDeviceIds.toString());
        // request each fog device to send its resource characteristics
        for (Integer fogDeviceId : this.fogDeviceIds) {
            log("Sending resource request to fog device: %s", fogDeviceId);
            sendNow(fogDeviceId, Constants.MsgTag.RESOURCE_REQUEST);
        }
    }

    protected void processIncomingTask(SimEvent event) {
        IncomingTaskMsg incomingTaskMsg = (IncomingTaskMsg) event.getData();
        Task task = incomingTaskMsg.getTask();

        task.setUserId(getId());
        this.waitingTaskQueue.add(task);
        this.tasks.put(task.getTaskId(), task);

        log("Task: %s of workflow: %s received", task.getTaskId(), task.getWorkflowId());
    }

    protected void processResourceRequestResponse(SimEvent event) {
        log("Resource received from fog device: %s", event.getSource());

        ResourceRequestResponseMsg responseMsg = (ResourceRequestResponseMsg) event.getData();

        this.fogDeviceIdToCharacteristics.put(event.getSource(), responseMsg.getCharacteristics());

        // if all fog devices have sent their characteristics, try to create vms
        if (this.fogDeviceIdToCharacteristics.size() == this.fogDeviceIds.size()) {
            log("All resources received");

            // map each requested vm to a fog device
            this.vmToFogDevice = this.vmToFogDeviceMapper.map(this.fogDeviceIdToCharacteristics, this.getVmList());
            log("Vm to FogDevice mapping: %s", this.vmToFogDevice.toString());

            // ask each fog device to create the vm assigned to it
            for (Integer vmId : this.vmToFogDevice.keySet()) {
                // the fog device that mapper chose for the vm with vmId
                var fogDeviceId = this.vmToFogDevice.get(vmId);

                log("Asking fog device: %s to create vm: %s", fogDeviceId, vmId);
                sendNow(
                        fogDeviceId, // send message to fog device
                        Constants.MsgTag.VM_CREATE, // tell fog device to create a vm
                        new VmCreateMsg(VmList.getById(this.getVmList(), vmId)) // vm
                );

                // add the fog device id to fog devices that has been requested to create a vm
                this.sentVmCreateRequests.add(fogDeviceId);
            }
        }
    }

    protected void processVmCreateAck(SimEvent event) {
        // add sender of the ack to fog devices that answered to creating the vm
        this.vmCreateAcks.add(event.getSource());

        VmCreateAckMsg ackMsg = (VmCreateAckMsg) event.getData();
        int vmId = ackMsg.getVmId();
        boolean isCreated = ackMsg.isCreated;

        log("Vm create ack received for vm: %s from fog device: %s with status: %s", vmId, event.getSource(), isCreated);

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

        log("Ack Received for task: %s with status: %s", taskId, isSubmitted);
    }

    protected void processVmDestroyAck(SimEvent event) {
        log("Vm destroy ack received from fog device: %s", event.getSource());
        this.vmDestroyAcks.add(event.getSource());

        if (this.vmDestroyAcks.containsAll(this.sentVmDestroyRequests)) {
            log("All vm destroy acks received");
            // now that resources are freed, create vms
            this.sentVmCreateRequests.clear();
            this.vmCreateAcks.clear();
            for (int vmId : this.toBeCreated) {
                var fogDeviceId = this.vmToFogDevice.get(vmId);

                log("Asking fog device: %s to create vm: %s", fogDeviceId, vmId);
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

        this.completedTasks.add(task);

        log("Received task completion msg for task: %s from fog device: %s", task.getTaskId(), event.getSource());

        // if all dispatched tasks are complete
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

            log("Vm provisioner decision:\n" +
                    "\tto be created: %s\n" +
                    "\tto be destroyed: %s\n" +
                    "\tto stay alive: %s\n", toBeCreated, toBeDestroyed, stayAlive);

            // Destroy vms
            log("Trying to destroy vms...");
            this.sentVmDestroyRequests.clear();
            this.vmDestroyAcks.clear();
            for (int vmId : toBeDestroyed) {
                var fogDeviceId = this.vmToFogDevice.get(vmId);

                log("Asking fog device: %s to destory vm: %s", fogDeviceId, vmId);
                sendNow(
                        fogDeviceId, // fog device containing the vm
                        Constants.MsgTag.VM_DESTROY, // tell the fog device to destroy the vm
                        new VmDestroyMsg(vmId)
                );
                this.sentVmDestroyRequests.add(fogDeviceId);
            }

            // if there is no vm to destroy
            if (this.sentVmDestroyRequests.isEmpty()) {
                log("There is no vm to destroy");
                // create vms
                log("Trying to create vms...");
                this.sentVmCreateRequests.clear();
                this.vmCreateAcks.clear();
                for (int vmId : this.toBeCreated) {
                    var fogDeviceId = this.vmToFogDevice.get(vmId);

                    log("Asking fog device: %s to create vm: %s", fogDeviceId, vmId);
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

            // add stayAlive vms to createdVms because they are already created
            this.createdVms = stayAlive.stream().map(vmId -> (Vm) VmList.getById(this.getVmList(), vmId)).collect(Collectors.toList());

            // if there is no vm to create either just execute the tasks
            if (this.sentVmCreateRequests.isEmpty()) {
                log("There is no vm to create");
                log("Trying to dispatch tasks...");
                dispatchTasks();
            }
        }
    }

    private void dispatchTasks() {
        // map each task in task queue to a vm(either a created or failed one)
        var newTaskToVm = this.taskToVmMapper.map(
                this.createdVms,
                this.failedVms,
                this.waitingTaskQueue,
                this.completedTasks,
                this.dispatchedTasks,
                this.taskToVm
        );

        // merge new mapping with the old one
        this.taskToVm.putAll(newTaskToVm);


        log("Tasks mapped to vms %s: {task_id=vm_id}", this.taskToVm);

        for (int taskId : this.taskToVm.keySet()) {
            this.tasks.get(taskId).setVmId(this.taskToVm.get(taskId));
        }

        for (Iterator<Task> it = this.waitingTaskQueue.iterator(); it.hasNext(); ) {
            Task task = it.next();

            // vm for current task
            int mappedVmId = this.taskToVm.get(task.getTaskId());

            List<Task> parentTasks = task.getParents().stream().map(this.tasks::get).collect(Collectors.toList());

            // if vm is successfully created, and all parent tasks are complete,
            // dispatch the task the to fog device which contains the vm
            if (this.createdVms.stream().anyMatch(vm -> vm.getId() == mappedVmId)
                    && this.completedTasks.containsAll(parentTasks)) {
                int dstFogDeviceId = this.vmToFogDevice.get(mappedVmId);

                // inform fog devices containing parent tasks to send output of parent tasks to the fog device containing the child task
                for (int parentId : task.getParents()) {
                    int fogDeviceId = this.vmToFogDevice.get(this.taskToVm.get(parentId));

                    log("Sending STAGE_OUT_MSG to fog device: %s for task: %s", fogDeviceId, parentId);
                    sendNow(
                            fogDeviceId,
                            Constants.MsgTag.STAGE_OUT_DATA,
                            new StageOutDataMsg(parentId, dstFogDeviceId)
                    );
                }

                // if task does not have any parent, its data must be stage in
                if (task.getParents().isEmpty()) {
                    double delay = (double) task.getTotalInputDataSize() / this.upLinkBw;
                    log("Sending STAGE_IN data for task: %s to fog device: %s with delay: %.2f", task.getTaskId(), dstFogDeviceId, delay);
                    send(
                            dstFogDeviceId,
                            delay,
                            Constants.MsgTag.EXECUTE_TASK_WITH_DATA,
                            new ExecuteTaskMsg(task, mappedVmId)
                    );
                } else {
                    log("Asking fog device: %s to execute task: %s", dstFogDeviceId, task.getTaskId());
                    sendNow(
                            dstFogDeviceId, // fog device containing the vm
                            Constants.MsgTag.EXECUTE_TASK, // tell fog device to execute the task
                            new ExecuteTaskMsg(task, mappedVmId) // send task with its corresponding vm
                    );
                }

                this.dispatchedTasks.add(task);

                // remove dispatched task from waiting queue
                it.remove();
            }
        }
    }

    @Override
    public <T extends Cloudlet> List<T> getCloudletReceivedList() {
        return (List<T>) new ArrayList<>(this.completedTasks);
    }

    private void log(String formatted, Object... args) {
        String tag = String.format("FogBroker(%s)", getId());

        Logger.log(tag, formatted, args);
    }
}
