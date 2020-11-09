package misty.entity;

import misty.computation.Task;
import misty.core.Constants;
import misty.mapper.TaskToVmMapper;
import misty.mapper.VmToFogDeviceMapper;
import misty.message.*;
import misty.provision.VmProvisioner;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;

import java.util.*;
import java.util.stream.Collectors;

public class FogBroker extends PowerDatacenterBroker {

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
    private Map<Integer, Integer> taskToVm;

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
        this.vmToFogDevice = new HashMap<>();
        this.dispatchedTasks = new HashSet<>();
        this.completedTasks = new HashSet<>();

        this.upLinkBw = upLinkBw;
        this.downLinkBw = downLinkBw;
    }

    @Override
    public void startEntity() {
        System.out.println("Starting Broker...");

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
            case Constants.MsgTag.TASK_IS_DONE:
                processTaskIsDone(event);
                break;
            default:
                processOtherEvent(event);
                break;
        }
    }

    @Override
    public void shutdownEntity() {

    }

    protected void init() {
        System.out.println("Broker: requesting for fog devices characteristics");

        // store id of all fog devices that registered their selves
        this.fogDeviceIds.addAll(CloudSim.getCloudResourceList());

        System.out.println("Broker: received fog device ids: " + this.fogDeviceIds.toString());

        // request each fog device to send its resource characteristics
        for (Integer fogDeviceId : this.fogDeviceIds) {
            System.out.println("Broker: sending resource request to fog device: " + fogDeviceId);
            sendNow(fogDeviceId, Constants.MsgTag.RESOURCE_REQUEST);
        }
    }

    protected void processIncomingTask(SimEvent event) {
        IncomingTaskMsg incomingTaskMsg = (IncomingTaskMsg) event.getData();
        Task task = incomingTaskMsg.getTask();

        task.setUserId(getId());
        this.waitingTaskQueue.add(task);
        this.tasks.put(task.getTaskId(), task);

        System.out.printf("Broker: task: %s of workflow: %s received\n", task.getTaskId(), task.getWorkflowId());
    }

    protected void processResourceRequestResponse(SimEvent event) {
        System.out.println("Broker: resource received from fog device: " + event.getSource());

        ResourceRequestResponseMsg responseMsg = (ResourceRequestResponseMsg) event.getData();

        this.fogDeviceIdToCharacteristics.put(event.getSource(), responseMsg.getCharacteristics());

        // if all fog devices have sent their characteristics, try to create vms
        if (this.fogDeviceIdToCharacteristics.size() == this.fogDeviceIds.size()) {
            System.out.println("Broker: all resources received");

            // map each requested vm to a fog device
            this.vmToFogDevice = this.vmToFogDeviceMapper.map(this.fogDeviceIdToCharacteristics, this.getVmList());
            System.out.println("Broker: vm to fog device mapping: " + this.vmToFogDevice.toString());

            // ask each fog device to create the vm assigned to it
            for (Integer vmId : this.vmToFogDevice.keySet()) {
                // the fog device that mapper chose for the vm with vmId
                var fogDeviceId = this.vmToFogDevice.get(vmId);

                System.out.printf("Broker: asking fog device: %s to create vm: %s", fogDeviceId, vmId);
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

        System.out.printf("Broker: vm ack received for vm: %s from fog device: %s with status: %s", vmId, event.getSource(), isCreated);

        if (isCreated)
            this.createdVms.add(VmList.getById(this.getVmList(), vmId));
        else
            this.failedVms.add(VmList.getById(this.getVmList(), vmId));

        // if all fog devices responded to vm creation requests, dispatch cloudlets
        if (this.vmCreateAcks.containsAll(this.sentVmCreateRequests)) {
            System.out.println("Broker: all acks for vm creation received");

            // map each task in task queue to a vm(either a created or failed one)
            this.taskToVm = this.taskToVmMapper.map(
                    this.createdVms,
                    this.failedVms,
                    this.waitingTaskQueue,
                    this.completedTasks,
                    this.dispatchedTasks,
                    this.taskToVm
            );

            for (int taskId : this.taskToVm.keySet()) {
                this.tasks.get(taskId).setVmId(this.taskToVm.get(taskId));
            }

            for (Task task : this.waitingTaskQueue) {
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

                        System.out.printf("Broker: sending STAGE_MSG to fog device: %s for task: %s", fogDeviceId, task.getTaskId());
                        sendNow(
                                fogDeviceId,
                                Constants.MsgTag.STAGE_OUT_DATA,
                                new StageOutDataMsg(task.getTaskId(), dstFogDeviceId)
                        );
                    }

                    // if task does not have any parent, its data must be stage in
                    if (task.getParents().isEmpty()) {
                        System.out.printf("Broker: sending STAGE_IN data for task: %s to fog device: %s", task.getTaskId(), dstFogDeviceId);
                        send(
                                dstFogDeviceId,
                                (double) task.getTotalInputDataSize() / this.upLinkBw,
                                Constants.MsgTag.EXECUTE_TASK_WITH_DATA,
                                new ExecuteTaskMsg(task, mappedVmId)
                        );
                    } else {
                        System.out.printf("Broker: asking fog device: %s to execute task: %s", dstFogDeviceId, task.getTaskId());
                        sendNow(
                                dstFogDeviceId, // fog device containing the vm
                                Constants.MsgTag.EXECUTE_TASK, // tell fog device to execute the task
                                new ExecuteTaskMsg(task, mappedVmId) // send task with its corresponding vm
                        );
                    }

                    this.dispatchedTasks.add(task);

                    // remove dispatched task from waiting queue
                    this.waitingTaskQueue.remove(task);
                }
            }
        }
    }

    protected void processVmDestroyAck(SimEvent event) {
        System.out.printf("Broker: vm destroy ack received from fog device: %s", event.getSource());
        this.vmDestroyAcks.add(event.getSource());

        if (this.vmDestroyAcks.containsAll(this.sentVmDestroyRequests)) {
            System.out.println("Broker: all vm destroy acks received");
            // now that resources are freed, create vms
            this.sentVmCreateRequests.clear();
            this.vmCreateAcks.clear();
            for (int vmId : this.toBeCreated) {
                var fogDeviceId = this.vmToFogDevice.get(vmId);

                System.out.printf("Broker: asking fog device: %s to create vm: %s", fogDeviceId, vmId);
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
        TaskIsDoneMsg doneMsg = (TaskIsDoneMsg) event.getData();
        Task task = doneMsg.getTask();

        this.completedTasks.add(task);
        System.out.printf("Broker: received task completion msg for task: %s from fog device: %s", task.getTaskId(), event.getSource());

        // if all dispatched tasks are complete
        if (this.dispatchedTasks.size() == this.completedTasks.size()) {
            System.out.println("Broker: all dispatched tasks are complete");

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

            System.out.printf("Broker: vm provision decision:\n" +
                    "to be created: %s\n" +
                    "to be destroyed: %s\n" +
                    "to stay alive: %s\n", toBeCreated, toBeDestroyed, stayAlive);

            // Destroy vms
            this.sentVmDestroyRequests.clear();
            this.vmDestroyAcks.clear();
            for (int vmId : toBeDestroyed) {
                var fogDeviceId = this.vmToFogDevice.get(vmId);

                System.out.printf("Broker: asking fog device: %s to destroy vm: %s", fogDeviceId, vmId);
                sendNow(
                        fogDeviceId, // fog device containing the vm
                        Constants.MsgTag.VM_DESTROY, // tell the fog device to destroy the vm
                        new VmDestroyMsg(vmId)
                );
                this.sentVmDestroyRequests.add(fogDeviceId);
            }

            this.createdVms.clear();
            this.failedVms.clear();

            // add stayAlive vms to createdVms because they are already created
            this.createdVms = stayAlive.stream().map(vmId -> (Vm) VmList.getById(this.getVmList(), vmId)).collect(Collectors.toList());
        }
    }
}
