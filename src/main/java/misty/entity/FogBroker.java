package misty.entity;

import misty.computation.Task;
import misty.core.Constants;
import misty.mapper.TaskToVmMapper;
import misty.mapper.VmToDatacenterMapper;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;
import org.jgrapht.alg.util.Pair;

import java.util.*;

public class FogBroker extends PowerDatacenterBroker {

    private final List<Task> taskQueue;
    private final List<Integer> fogDeviceIds;
    private final Map<Integer, DatacenterCharacteristics> fogDeviceIdToCharacteristics;

    // variables for vm management
    private final VmToDatacenterMapper vmToDatacenterMapper;
    private Map<Integer, Integer> vmToDatacenter;
    private final Set<Integer> requestedFromDatacenters;
    private final Set<Integer> ackFromDatacenters;
    private final List<Vm> createdVms;
    private final List<Vm> failedVms;

    // variables for task management
    private final TaskToVmMapper taskToVmMapper;
    private Map<Integer, Integer> taskToVm;

    public FogBroker(String name, VmToDatacenterMapper vmToDatacenterMapper, TaskToVmMapper taskToVmMapper) throws Exception {
        super(name);

        this.taskQueue = new ArrayList<>();
        this.fogDeviceIds = new ArrayList<>();
        this.fogDeviceIdToCharacteristics = new HashMap<>();

        this.vmToDatacenterMapper = vmToDatacenterMapper;
        this.requestedFromDatacenters = new HashSet<>();
        this.ackFromDatacenters = new HashSet<>();
        this.createdVms = new ArrayList<>();
        this.failedVms = new ArrayList<>();

        this.taskToVmMapper = taskToVmMapper;
    }

    @Override
    public void startEntity() {
        System.out.println("Starting Broker...");
        schedule(getId(), 0, Constants.MsgTag.INIT);
    }

    @Override
    public void processEvent(SimEvent event) {
        switch (event.getTag()) {
            // request each fog device for its characteristics
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
                processVmAck(event);
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
                sendNow(
                        this.vmToDatacenter.get(vmId), // datacenter id
                        Constants.MsgTag.VM_CREATE, // tell datacenter to create a vm
                        VmList.getById(this.getVmList(), vmId) // vm
                );
                this.requestedFromDatacenters.add(this.vmToDatacenter.get(vmId));
            }

        }
    }

    protected void processVmAck(SimEvent event) {
        System.out.println("Broker: Vm ack received from fog device: " + event.getSource());
        this.ackFromDatacenters.add(event.getSource());

        int[] data = (int[]) event.getData();
        int datacenterId = data[0];
        int vmId = data[1];
        boolean isCreated = data[2] == 1;

        if (isCreated)
            this.createdVms.add(VmList.getById(this.getVmList(), vmId));
        else
            this.failedVms.add(VmList.getById(this.getVmList(), vmId));

        if (this.ackFromDatacenters.containsAll(this.requestedFromDatacenters)) {
            System.out.println("Broker: all acks for vm creation received");

            this.taskToVm = this.taskToVmMapper.map(this.createdVms, this.failedVms, this.taskQueue);

            for (Task task : this.taskQueue){
                int mappedVmId = this.taskToVm.get(task.getTaskId());
                if (this.createdVms.stream().anyMatch(vm -> vm.getId() == mappedVmId)) {
                    sendNow(
                            this.vmToDatacenter.get(mappedVmId),
                            Constants.MsgTag.EXECUTE_TASK,
                            Pair.of(task, mappedVmId)
                    );

                    this.taskQueue.remove(task);
                }
            }

        }
    }
}
