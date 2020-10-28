package misty.entity;

import misty.computation.Task;
import misty.core.Constants;
import misty.message.*;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;

import java.util.*;

public class FogDevice extends PowerDatacenter {

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
        Log.printConcatLine("FogDevice: " + getId(), " is starting...");

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
            case Constants.MsgTag.STAGE_OUT_DATA:
                processStageOutData(event);
                break;
            case Constants.MsgTag.EXECUTE_TASK:
                processExecuteTask(event);
                break;
            case Constants.MsgTag.FOG_TO_FOG:
                processFogToFog(event);
            case Constants.MsgTag.DOWNLOAD_FOG_TO_FOG:
                processDownloadedFogToFog(event);
                break;
            default:
                processOtherEvent(event);
                break;
        }
    }

    protected void processInit() {
        List<Integer> fogDeviceIds = CloudSim.getCloudResourceList();

        // broadcast name and id of the fog device to other fog devices
        for (int fogDeviceId : fogDeviceIds)
            if (fogDeviceId != getId())
                sendNow(fogDeviceId, Constants.MsgTag.BROADCAST_ID, new BroadcastIdMsg(this.getName(), this.getId()));
    }

    protected void processResourceRequest(SimEvent event) {
        sendNow(event.getSource(), Constants.MsgTag.RESOURCE_REQUEST_RESPONSE, new ResourceRequestResponseMsg(this.getCharacteristics()));
    }

    protected void processBroadcastId(SimEvent event) {
        BroadcastIdMsg broadcastMsg = (BroadcastIdMsg) event.getData();

        this.nameToId.put(broadcastMsg.getName(), broadcastMsg.getId());
        this.idToName.put(broadcastMsg.getId(), broadcastMsg.getName());
    }

    protected void processStageOutData(SimEvent event) {
        StageOutDataMsg stageOutDataMsg = (StageOutDataMsg) event.getData();

        String dstName = this.idToName.get(stageOutDataMsg.getDstFogDeviceId());

        String nextHopName = this.nextHop(dstName);

        int nextHopId = this.nameToId.get(nextHopName);

        Task task = this.tasks.get(stageOutDataMsg.getTaskId());

        send(
                nextHopId,
                (double) task.getCloudletOutputSize() / this.upLinkBw,
                Constants.MsgTag.FOG_TO_FOG,
                new FogToFogMsg(stageOutDataMsg.getDstFogDeviceId(), stageOutDataMsg.getTaskId(), task.getCloudletOutputSize())
        );
    }

    protected void processFogToFog(SimEvent event) {
        FogToFogMsg fogToFogMsg = (FogToFogMsg) event.getData();

        send(getId(), (double) fogToFogMsg.getData() / this.downLinkBw, Constants.MsgTag.DOWNLOAD_FOG_TO_FOG, fogToFogMsg);
    }

    protected void processDownloadedFogToFog(SimEvent event) {
        FogToFogMsg fogToFogMsg = (FogToFogMsg) event.getData();

        if (this.getId() == fogToFogMsg.getDstFogDeviceId()) {
            this.receivedData.add(fogToFogMsg.getTaskId());

            // iterate over waiting tasks and check if any of them can be executed
            for (int taskId : this.waitingTasks) {
                Task task = this.tasks.get(taskId);

                if (this.receivedData.containsAll(task.getParents())) {
                    sendNow(
                            getId(),
                            Constants.MsgTag.EXECUTE_TASK,
                            new ExecuteTaskMsg(task, task.getVmId())
                    );
                }
            }
        } else {
            String dstName = this.idToName.get(fogToFogMsg.getDstFogDeviceId());

            String nextHopName = this.nextHop(dstName);

            int nextHopId = this.nameToId.get(nextHopName);

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

        this.tasks.put(executeTaskMsg.getTask().getTaskId(), executeTaskMsg.getTask());

        // check if all input data is provided
        if (this.receivedData.containsAll(executeTaskMsg.getTask().getParents())) {
            // execute the task
            sendNow(
                    getId(),
                    CloudSimTags.CLOUDLET_SUBMIT,
                    executeTaskMsg.getTask()
            );

            this.waitingTasks.remove(executeTaskMsg.getTask().getTaskId());
        } else {
            this.waitingTasks.add(executeTaskMsg.getTask().getTaskId());
        }
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

}
