package misty.entity;

import misty.computation.Task;
import misty.core.Constants;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FogBroker extends PowerDatacenterBroker {

    private List<Task> taskQueue;
    private List<Integer> fogDeviceIds;
    private Map<Integer, DatacenterCharacteristics> fogDeviceIdToCharacteristics;

    public FogBroker(String name) throws Exception {
        super(name);

        this.taskQueue = new ArrayList<>();
        this.fogDeviceIds = new ArrayList<>();
        this.fogDeviceIdToCharacteristics = new HashMap<>();
    }

    @Override
    public void startEntity() {
        System.out.println("Starting Broker...");
        schedule(getId(), 0, Constants.MsgTag.INIT);
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            // request each fog device for its characteristics
            case Constants.MsgTag.INIT:
                init(ev);
                break;
            // process incoming task from Task Manager
            case Constants.MsgTag.INCOMING_TASK:
                processIncomingTask(ev);
                break;
            case Constants.MsgTag.INCOMING_RESOURCE_CHARACTERISTICS:
                break;
            default:
                processOtherEvent(ev);
                break;
        }
    }

    @Override
    public void shutdownEntity() {

    }

    protected void processIncomingTask(SimEvent event) {
        Task task = (Task) event.getData();
        this.taskQueue.add(task);

        System.out.printf("Broker: task: %s of workflow: %s received\n", task.getTaskId(), task.getWorkflowId());
    }

    protected void init(SimEvent event) {
        System.out.println("Broker: requesting for fog devices characteristics");

        this.fogDeviceIds.addAll(CloudSim.getCloudResourceList());

        // request each fog device to send its resource characteristics
        for (Integer fogDeviceId : this.fogDeviceIds) {
            sendNow(fogDeviceId, Constants.MsgTag.RESOURCE_REQUEST);
        }
    }

    protected void processIncomingResourceCharacteristics(SimEvent event) {
        System.out.println("Resource characteristics received");
    }
}
