package misty.entity;

import misty.computation.Task;
import misty.core.Constants;
import misty.core.Logger;
import misty.message.*;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.*;

public class FogDevice extends Datacenter {

    private final long upLinkBw;
    private final long downLinkBw;

    private final HashMap<String, Integer> nameToId;
    private final HashMap<Integer, String> idToName;

    private final HashMap<String, String> routingTable;

    private final List<String> neighbors;

    private final Map<Integer, Task> tasks;
    private final List<Integer> waitingTasks;
    private final HashSet<Integer> receivedData;

    public FogDevice(String name,
                     DatacenterCharacteristics characteristics,
                     VmAllocationPolicy vmAllocationPolicy,
                     List<Storage> storageList,
                     double schedulingInterval,
                     List<String> neighbors,
                     long upLinkBw,
                     long downLinkBw) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);

        this.neighbors = neighbors;
        this.routingTable = new HashMap<>();

        this.nameToId = new HashMap<>();
        this.idToName = new HashMap<>();

        this.tasks = new HashMap<>();
        this.waitingTasks = new ArrayList<>();
        this.receivedData = new HashSet<>();

        this.upLinkBw = upLinkBw;
        this.downLinkBw = downLinkBw;
    }

    @Override
    public void startEntity() {
        log("Device is starting...");

        // this resource should register to regional CIS.
        // However, if not specified, then register to system CIS (the
        // default CloudInformationService) entity.
        int gisID = CloudSim.getEntityId(this.getRegionalCisName());
        if (gisID == -1) {
            gisID = CloudSim.getCloudInfoServiceEntityId();
        }

        // send the registration to CIS
        sendNow(gisID, CloudSimTags.REGISTER_RESOURCE, getId());

        // Below method is for a child class to override
        registerOtherEntity();

        sendNow(getId(), Constants.MsgTag.INIT);
    }

    @Override
    public void processEvent(SimEvent event) {
        switch (event.getTag()) {
            case Constants.MsgTag.INIT:
                processInit();
                break;
            case Constants.MsgTag.RESOURCE_REQUEST:
                processResourceRequest(event);
                break;
            case Constants.MsgTag.BROADCAST_ID:
                processBroadcastId(event);
                break;
            case Constants.MsgTag.VM_CREATE:
                processVmCreate(event);
                break;
            case Constants.MsgTag.STAGE_OUT_DATA:
                processStageOutData(event);
                break;
            case Constants.MsgTag.EXECUTE_TASK_WITH_DATA:
                processExecuteTaskWithData(event);
                break;
            case Constants.MsgTag.EXECUTE_TASK:
                processExecuteTask(event);
                break;
            case Constants.MsgTag.FOG_TO_FOG:
                processFogToFog(event);
                break;
            case Constants.MsgTag.DOWNLOADED_FOG_TO_FOG:
                processDownloadedFogToFog(event);
                break;
            default:
                super.processEvent(event);
                break;
        }
    }

    protected void processInit() {
        log("Initializing fog device");
        List<Integer> fogDeviceIds = CloudSim.getCloudResourceList();

        // broadcast name and id of the fog device to other fog devices
        for (int fogDeviceId : fogDeviceIds) {

            log("Broadcasting name and id");
            if (fogDeviceId != getId())
                sendNow(fogDeviceId, Constants.MsgTag.BROADCAST_ID, new BroadcastIdMsg(this.getName(), this.getId()));
        }
    }

    protected void processVmCreate(SimEvent event) {
        VmCreateMsg vmCreateMsg = (VmCreateMsg) event.getData();

        Vm vm = vmCreateMsg.getVm();

        log("Receiving vm create msg for vm: %s, with user id: %s", vm.getId(), vm.getUserId());

        boolean result = getVmAllocationPolicy().allocateHostForVm(vm);

        log("Result of vm creation: %s", result);
        log("Sending ack to broker");
        sendNow(event.getSource(), Constants.MsgTag.VM_CREATE_ACK, new VmCreateAckMsg(getId(), vm.getId(), result));

        if (result) {
            getVmList().add(vm);

            if (vm.isBeingInstantiated()) {
                vm.setBeingInstantiated(false);
            }

            vm.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(vm).getVmScheduler().getAllocatedMipsForVm(vm));
        }

    }

    protected void processResourceRequest(SimEvent event) {
        log("Sending resources");
        sendNow(event.getSource(), Constants.MsgTag.RESOURCE_REQUEST_RESPONSE, new ResourceRequestResponseMsg(this.getCharacteristics()));
    }

    protected void processBroadcastId(SimEvent event) {
        BroadcastIdMsg broadcastMsg = (BroadcastIdMsg) event.getData();

        log("received name: %s and id: %s from fog device: %s", broadcastMsg.getName(), broadcastMsg.getId(), event.getSource());
        this.nameToId.put(broadcastMsg.getName(), broadcastMsg.getId());
        this.idToName.put(broadcastMsg.getId(), broadcastMsg.getName());
    }

    protected void processStageOutData(SimEvent event) {
        StageOutDataMsg stageOutDataMsg = (StageOutDataMsg) event.getData();

        if (stageOutDataMsg.getDstFogDeviceId() == this.getId()) {
            log("Received STAGE_OUT for task: %s and destination is a loop back: sending data instantly", stageOutDataMsg.getTaskId());
            sendNow(
                    getId(),
                    Constants.MsgTag.DOWNLOADED_FOG_TO_FOG,
                    new FogToFogMsg(getId(), stageOutDataMsg.getTaskId(), this.tasks.get(stageOutDataMsg.getTaskId()).getCloudletOutputSize())
            );

            return;
        }

        String dstName = this.idToName.get(stageOutDataMsg.getDstFogDeviceId());
        log("Received STAGE_OUT data msg to fog device(%s,%s)", dstName, stageOutDataMsg.getDstFogDeviceId());

        String nextHopName = this.nextHop(dstName);

        int nextHopId = this.nameToId.get(nextHopName);
        log("Next hop is fog device: (%s,%s)", nextHopName, nextHopId);
        Task task = this.tasks.get(stageOutDataMsg.getTaskId());

        log("Uploading data to fog device(%s,%s)", nextHopName, nextHopId);
        send(
                nextHopId,
                (double) task.getCloudletOutputSize() / this.upLinkBw,
                Constants.MsgTag.FOG_TO_FOG,
                new FogToFogMsg(stageOutDataMsg.getDstFogDeviceId(), stageOutDataMsg.getTaskId(), task.getCloudletOutputSize())
        );
    }

    protected void processFogToFog(SimEvent event) {
        FogToFogMsg fogToFogMsg = (FogToFogMsg) event.getData();

        log("Downloading data from fog device: %s", event.getSource());
        send(getId(), (double) fogToFogMsg.getData() / this.downLinkBw, Constants.MsgTag.DOWNLOADED_FOG_TO_FOG, fogToFogMsg);
    }

    protected void processDownloadedFogToFog(SimEvent event) {
        FogToFogMsg fogToFogMsg = (FogToFogMsg) event.getData();
        log("Downloaded data of task %s", fogToFogMsg.getTaskId());

        if (this.getId() == fogToFogMsg.getDstFogDeviceId()) {
            log("Data is for me");
            this.receivedData.add(fogToFogMsg.getTaskId());

            // iterate over waiting tasks and check if any of them can be executed
            for (int taskId : this.waitingTasks) {
                Task task = this.tasks.get(taskId);

                if (this.receivedData.containsAll(task.getParents())) {
                    log("Found task: %s from workflow: %s that can be executed", task.getTaskId(), task.getWorkflowId());
                    sendNow(
                            getId(),
                            Constants.MsgTag.EXECUTE_TASK,
                            new ExecuteTaskMsg(task, task.getVmId())
                    );
                }
            }
        } else {
            log("Data is not for me. relaying the message");

            String dstName = this.idToName.get(fogToFogMsg.getDstFogDeviceId());

            String nextHopName = this.nextHop(dstName);

            int nextHopId = this.nameToId.get(nextHopName);

            log("Uploading data to next hop: (%s,%s) with destination: (%s,%s)", nextHopName, nextHopId, dstName, fogToFogMsg.getDstFogDeviceId());
            send(
                    nextHopId,
                    (double) fogToFogMsg.getData() / this.upLinkBw,
                    Constants.MsgTag.FOG_TO_FOG,
                    fogToFogMsg
            );
        }
    }

    protected void processExecuteTask(SimEvent event) {
        ExecuteTaskMsg executeTaskMsg = (ExecuteTaskMsg) event.getData();

        log("Received execute msg for task: %s from workflow: %s to run on vm with id: %s", executeTaskMsg.getTask().getTaskId(), executeTaskMsg.getTask().getWorkflowId(), executeTaskMsg.getVmId());
        this.tasks.put(executeTaskMsg.getTask().getTaskId(), executeTaskMsg.getTask());

        // check if all input data is provided
        if (this.receivedData.containsAll(executeTaskMsg.getTask().getParents())) {
            log("All parent data for task: %s are received. task can execute", executeTaskMsg.getTask().getTaskId());
            // execute the task
            sendNow(
                    getId(),
                    CloudSimTags.CLOUDLET_SUBMIT_ACK,
                    executeTaskMsg.getTask()
            );

            this.waitingTasks.removeIf(taskId -> taskId == executeTaskMsg.getTask().getTaskId());
        } else {
            log("Not all parent data are available. task is added to waiting queue");
            this.waitingTasks.add(executeTaskMsg.getTask().getTaskId());
        }
    }

    protected void processExecuteTaskWithData(SimEvent event) {
        ExecuteTaskMsg executeTaskMsg = (ExecuteTaskMsg) event.getData();

        log("Received task: %s from workflow: %s with STAGE_IN data to run on vm: %s", executeTaskMsg.getTask().getTaskId(), executeTaskMsg.getTask().getWorkflowId(), executeTaskMsg.getVmId());

        double delay = (double) executeTaskMsg.getTask().getTotalInputDataSize() / this.downLinkBw;
        log("Downloading STAGE_IN data with delay: %s", delay);

        send(
                getId(),
                delay,
                Constants.MsgTag.EXECUTE_TASK,
                executeTaskMsg
        );
    }

    public List<FogHost> getHosts() {
        return getCharacteristics().getHostList();
    }

    public List<String> getNeighbors() {
        return neighbors;
    }

    public void addRoute(String dst, String hop) {
        routingTable.put(dst, hop);
    }

    public String nextHop(String dst) {
        return routingTable.getOrDefault(dst, null);
    }

    private void log(String formatted, Object... args) {
        String tag = String.format("FogDevice(%s,%s)", getName(), getId());

        Logger.log(tag, formatted, args);
    }

}
