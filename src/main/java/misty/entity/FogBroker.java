package misty.entity;

import misty.computation.Task;
import misty.core.Constants;
import misty.mapper.TaskToVmMapper;
import misty.mapper.VmToDatacenterMapper;
import misty.provision.VmProvisioner;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;
import org.jgrapht.alg.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class FogBroker extends PowerDatacenterBroker {

    private final List<Task> taskQueue;
    private final List<Integer> fogDeviceIds;
    private final Map<Integer, DatacenterCharacteristics> fogDeviceIdToCharacteristics;

    // variables for vm management
    private final VmProvisioner vmProvisioner;
    private final VmToDatacenterMapper vmToDatacenterMapper;
    private final Set<Integer> vmCreateRequestedFromDatacenter;
    private final Set<Integer> vmCreateAckFromDatacenters;
    private final Set<Integer> vmDestroyRequestedFromDatacenter;
    private final Set<Integer> vmDestroyAckFromDatacenters;
    private final List<Vm> failedVms;

    // variables for task management
    private final TaskToVmMapper taskToVmMapper;
    private List<Vm> createdVms;
    private Map<Integer, Integer> vmToDatacenter;
    private Map<Integer, Integer> taskToVm;
    private final Set<Task> dispatchedTasks;
    private final Set<Task> completedTasks;

    public FogBroker(String name, VmProvisioner vmProvisioner, VmToDatacenterMapper vmToDatacenterMapper, TaskToVmMapper taskToVmMapper) throws Exception {
        super(name);

        this.taskQueue = new ArrayList<>();
        this.fogDeviceIds = new ArrayList<>();
        this.fogDeviceIdToCharacteristics = new HashMap<>();

        this.vmProvisioner = vmProvisioner;
        this.vmToDatacenterMapper = vmToDatacenterMapper;
        this.vmCreateRequestedFromDatacenter = new HashSet<>();
        this.vmCreateAckFromDatacenters = new HashSet<>();
        this.vmDestroyRequestedFromDatacenter = new HashSet<>();
        this.vmDestroyAckFromDatacenters = new HashSet<>();
        this.createdVms = new ArrayList<>();
        this.failedVms = new ArrayList<>();

        this.taskToVmMapper = taskToVmMapper;
        this.vmToDatacenter = new HashMap<>();
        this.dispatchedTasks = new HashSet<>();
        this.completedTasks = new HashSet<>();
    }

    @Override
    public void startEntity() {
        System.out.println("Starting Broker...");

        // broker sends INIT message to itself to start itself
        schedule(getId(), 0, Constants.MsgTag.INIT);
    }

    @Override
    public void processEvent(SimEvent event) {
        switch (event.getTag()) {
            // start the broker and request each fog device for its characteristics
            case Constants.MsgTag.INIT:
                init(event);
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

    protected void init(SimEvent event) {
        System.out.println("Broker: requesting for fog devices characteristics");

        this.fogDeviceIds.addAll(CloudSim.getCloudResourceList());

        // request each fog device to send its resource characteristics
        for (Integer fogDeviceId : this.fogDeviceIds) {
            sendNow(fogDeviceId, Constants.MsgTag.RESOURCE_REQUEST);
        }
    }

    protected void processIncomingTask(SimEvent event) {
        Task task = (Task) event.getData();
        this.taskQueue.add(task);

        System.out.printf("Broker: task: %s of workflow: %s received\n", task.getTaskId(), task.getWorkflowId());
    }

    protected void processResourceRequestResponse(SimEvent event) {
        System.out.println("Broker: resource received from fog device: " + event.getSource());

        this.fogDeviceIdToCharacteristics.put(event.getSource(), (DatacenterCharacteristics) event.getData());

        // if all fog devices have sent their characteristics, try to create vms
        if (this.fogDeviceIdToCharacteristics.size() == this.fogDeviceIds.size()) {
            System.out.println("Broker: all resources received");

            // map each requested vm to a datacenter
            this.vmToDatacenter = this.vmToDatacenterMapper.map(this.fogDeviceIdToCharacteristics, this.getVmList());

            // ask each datacenter to create the vm assigned to it
            for (Integer vmId : this.vmToDatacenter.keySet()) {
                // datacenter the mapper chose for the vm
                var datacenterId = this.vmToDatacenter.get(vmId);
                sendNow(
                        datacenterId, // send message to datacenter
                        Constants.MsgTag.VM_CREATE, // tell datacenter to create a vm
                        VmList.getById(this.getVmList(), vmId) // vm
                );

                // add the datacenter to datacenters that has been requested to create a vm
                this.vmCreateRequestedFromDatacenter.add(datacenterId);
            }

        }
    }

    protected void processVmCreateAck(SimEvent event) {
        System.out.println("Broker: Vm ack received from fog device: " + event.getSource());

        // add sender of the ack to datacenters that answered to creating the vm
        this.vmCreateAckFromDatacenters.add(event.getSource());

        int[] data = (int[]) event.getData();
        int datacenterId = data[0];
        int vmId = data[1];
        boolean isCreated = data[2] == 1;

        if (isCreated)
            this.createdVms.add(VmList.getById(this.getVmList(), vmId));
        else
            this.failedVms.add(VmList.getById(this.getVmList(), vmId));

        // if all datacenters responded to vm creation requests, dispatch cloudlets
        if (this.vmCreateAckFromDatacenters.containsAll(this.vmCreateRequestedFromDatacenter)) {
            System.out.println("Broker: all acks for vm creation received");

            this.taskToVm = this.taskToVmMapper.map(this.createdVms, this.failedVms, this.taskQueue, this.taskToVm);

            for (Task task : this.taskQueue) {
                // vm for current task
                int mappedVmId = this.taskToVm.get(task.getTaskId());

                // if vm is successfully created, dispatch the task the to datacenter which contains the vm
                if (this.createdVms.stream().anyMatch(vm -> vm.getId() == mappedVmId)) {
                    sendNow(
                            this.vmToDatacenter.get(mappedVmId), // datacenter containing the vm
                            Constants.MsgTag.EXECUTE_TASK, // tell datacenter to execute the task
                            Pair.of(task, mappedVmId) // send task with its corresponding vm
                    );

                    this.dispatchedTasks.add(task);

                    // remove dispatched task from waiting queue
                    this.taskQueue.remove(task);
                }
            }
        }
    }

    protected void processVmDestroyAck(SimEvent event) {
        this.vmDestroyAckFromDatacenters.add(event.getSource());

        if (this.vmDestroyAckFromDatacenters.containsAll(this.vmDestroyRequestedFromDatacenter)) {
            System.out.println("All requested vms destroyed");
        }
    }

    protected void processTaskIsDone(SimEvent event) {
        Task task = (Task) event.getData();

        this.completedTasks.add(task);

        // all dispatched tasks are complete now
        if (this.dispatchedTasks.size() == this.completedTasks.size()) {
            var triple = this.vmProvisioner.provision(
                    this.failedVms,
                    this.createdVms,
                    this.getVmList(),
                    this.taskToVm,
                    this.completedTasks,
                    this.taskQueue
            );

            var toBeCreated = triple.getFirst();
            var toBeDestroyed = triple.getSecond();
            var stayAlive = triple.getThird();

            // Destroy vms
            this.vmDestroyRequestedFromDatacenter.clear();
            this.vmDestroyAckFromDatacenters.clear();
            for (int vmId : toBeDestroyed) {
                var datacenterId= this.vmToDatacenter.get(vmId);
                sendNow(
                        datacenterId,
                        Constants.MsgTag.VM_DESTROY,
                        vmId
                );
                this.vmDestroyRequestedFromDatacenter.add(datacenterId);
            }

            // Create vms
            this.vmCreateRequestedFromDatacenter.clear();
            this.vmCreateAckFromDatacenters.clear();
            for (int vmId : toBeCreated) {
                var datacenterId = this.vmToDatacenter.get(vmId);
                sendNow(
                        this.vmToDatacenter.get(vmId),
                        Constants.MsgTag.VM_CREATE,
                        vmId
                );
                this.vmCreateRequestedFromDatacenter.add(datacenterId);
            }

            this.createdVms.clear();
            this.failedVms.clear();

            // add stayAlive vms to createdVms
            this.createdVms = stayAlive.stream().map(vmId -> (Vm) VmList.getById(this.getVmList(), vmId)).collect(Collectors.toList());
        }
    }
}
