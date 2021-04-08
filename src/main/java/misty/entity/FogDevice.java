package misty.entity;

import misty.computation.Task;
import misty.core.Constants;
import misty.core.Logger;
import misty.message.*;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.jgrapht.alg.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class FogDevice extends Datacenter {

    private final long upLinkBw;
    private final long downLinkBw;

    private final HashMap<String, Integer> nameToId;
    private final HashMap<Integer, String> idToName;

    private final HashMap<String, String> routingTable;

    private final List<String> neighbors;

    private final Map<Integer, Task> tasks;
    private final List<Integer> waitingTasks;

    // cycle -> (taskId, isData)
    private final HashMap<Integer, HashSet<Pair<Integer, Boolean>>> receivedData;

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
        this.receivedData = new HashMap<>();

        this.upLinkBw = upLinkBw;
        this.downLinkBw = downLinkBw;
    }

    @Override
    public void startEntity() {
        log("Starting FogDevice(%s)...", getId());

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
            case Constants.MsgTag.INIT -> processInit();
            case Constants.MsgTag.RESOURCE_REQUEST -> processResourceRequest(event);
            case Constants.MsgTag.BROADCAST_ID -> processBroadcastId(event);
            case Constants.MsgTag.VM_CREATE -> processVmCreate(event);
            case Constants.MsgTag.STAGE_OUT_DATA -> processStageOutData(event);
            case Constants.MsgTag.EXECUTE_TASK_WITH_DATA -> processExecuteTaskWithData(event);
            case Constants.MsgTag.EXECUTE_TASK -> processExecuteTask(event);
            case Constants.MsgTag.FOG_TO_FOG -> processFogToFog(event);
            case Constants.MsgTag.DOWNLOADED_FOG_TO_FOG -> processDownloadedFogToFog(event);
            default -> super.processEvent(event);
        }
    }

    protected void processInit() {
        List<Integer> fogDeviceIds = CloudSim.getCloudResourceList();

        // broadcast name and id of the fog device to other fog devices
        for (int fogDeviceId : fogDeviceIds) {

            if (fogDeviceId != getId())
                sendNow(fogDeviceId, Constants.MsgTag.BROADCAST_ID, new BroadcastIdMsg(this.getName(), this.getId()));
        }
    }

    protected void processVmCreate(SimEvent event) {
        VmCreateMsg vmCreateMsg = (VmCreateMsg) event.getData();

        Vm vm = vmCreateMsg.getVm();

        boolean result = getVmAllocationPolicy().allocateHostForVm(vm);

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
        sendNow(event.getSource(), Constants.MsgTag.RESOURCE_REQUEST_RESPONSE, new ResourceRequestResponseMsg(this.getCharacteristics()));
    }

    protected void processBroadcastId(SimEvent event) {
        BroadcastIdMsg broadcastMsg = (BroadcastIdMsg) event.getData();

        this.nameToId.put(broadcastMsg.getName(), broadcastMsg.getId());
        this.idToName.put(broadcastMsg.getId(), broadcastMsg.getName());
    }

    protected void processStageOutData(SimEvent event) {
        StageOutDataMsg stageOutDataMsg = (StageOutDataMsg) event.getData();

        // If destination is this fog device
        if (stageOutDataMsg.getDstFogDeviceId() == this.getId()) {
            log("Received STAGE_OUT data of Task(%s) on Cycle(%s) and destination is a loop back: sending data instantly", stageOutDataMsg.getTaskId(),
                    stageOutDataMsg.getCycle());
            sendNow(
                    getId(),
                    Constants.MsgTag.DOWNLOADED_FOG_TO_FOG,
                    new FogToFogMsg(
                            getId(),
                            stageOutDataMsg.getTaskId(),
                            stageOutDataMsg.getCycle(),
                            this.tasks.get(stageOutDataMsg.getTaskId()).getCloudletOutputSize(),
                            stageOutDataMsg.isData()
                    )
            );

            return;
        }

        // If destination is not this fog device, use routing table and send the message to next hop
        String dstName = this.idToName.get(stageOutDataMsg.getDstFogDeviceId());
        log("Received STAGE_OUT data of Task(%s) on Cycle(%s) to FogDevice(%s)", stageOutDataMsg.getTaskId(), this.tasks.get(stageOutDataMsg.getTaskId()).getCycle(), stageOutDataMsg.getDstFogDeviceId());

        String nextHopName = this.nextHop(dstName);

        int nextHopId = this.nameToId.get(nextHopName);

        Task task = this.tasks.get(stageOutDataMsg.getTaskId());

        send(
                nextHopId,
                stageOutDataMsg.isData() ? (double) task.getCloudletOutputSize() / this.upLinkBw : 0,
                Constants.MsgTag.FOG_TO_FOG,
                new FogToFogMsg(
                        stageOutDataMsg.getDstFogDeviceId(),
                        stageOutDataMsg.getTaskId(),
                        stageOutDataMsg.getCycle(),
                        task.getCloudletOutputSize(),
                        stageOutDataMsg.isData()
                )
        );
    }

    protected void processFogToFog(SimEvent event) {
        FogToFogMsg fogToFogMsg = (FogToFogMsg) event.getData();

        log("Downloading data from fog device: %s", event.getSource());
        send(
                getId(),
                fogToFogMsg.isData() ? (double) fogToFogMsg.getData() / this.downLinkBw : 0,
                Constants.MsgTag.DOWNLOADED_FOG_TO_FOG,
                fogToFogMsg
        );
    }

    protected void processDownloadedFogToFog(SimEvent event) {
        FogToFogMsg fogToFogMsg = (FogToFogMsg) event.getData();
        log("Downloaded data of Task(%s) on Cycle(%s)", fogToFogMsg.getTaskId(), fogToFogMsg.getCycle());

        if (this.getId() == fogToFogMsg.getDstFogDeviceId()) {
            log("Data is for me");

            this.receivedData.computeIfAbsent(fogToFogMsg.getCycle(), k -> new HashSet<>());

            this.receivedData.get(fogToFogMsg.getCycle()).add(Pair.of(fogToFogMsg.getTaskId(), fogToFogMsg.isData()));

            // Iterate over waiting tasks and check if any of them can be executed
            for (Iterator<Integer> it = waitingTasks.iterator(); it.hasNext(); ) {
                int taskId = it.next();

                Task task = this.tasks.get(taskId);

                // Create a list of (parentId, isData=true)
                var idToTrue = task.getParents().stream().map(id -> Pair.of(id, true)).collect(Collectors.toList());

                if (this.receivedData.get(task.getCycle()).containsAll(idToTrue)) {
                    log("Found Task(%s) from Workflow(%s) that can be executed", task.getTaskId(), task.getWorkflowId());

                    task.getTaskState().setStartExecutionTime(task.getCycle(), CloudSim.clock());
                    sendNow(
                            getId(),
                            Constants.MsgTag.EXECUTE_TASK,
                            new ExecuteTaskMsg(task, task.getVmId())
                    );

                    task.getTaskState().setExitFogDeviceWaitingQueue(task.getCycle(), CloudSim.clock());
                    it.remove();
                }
                else if (this.receivedData.get(task.getCycle()).stream().anyMatch(pair -> task.getParents().contains(pair.getFirst()) && !pair.getSecond())) {
                    log("One of parents of Task(%s) did not generate data on Cycle(%s). Sending back Task...", task.getTaskId(), task.getCycle());

                    task.getCycleToGeneratedData().put(task.getCycle(), false);

                    task.getTaskState().setStartExecutionTime(task.getCycle(), CloudSim.clock());
                    sendNow(
                            task.getUserId(),
                            CloudSimTags.CLOUDLET_RETURN,
                            task
                    );

                    task.getTaskState().setExitFogDeviceWaitingQueue(task.getCycle(), CloudSim.clock());
                    it.remove();
                }
            }
        } else {
            log("Data is not for me. relaying the message");

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
        Task task = executeTaskMsg.getTask();

        log("Received execute msg for Task(%s) on Cycle(%s) from Workflow(%s) to run on Vm(%s)", task.getTaskId(), task.getCycle(), task.getWorkflowId(), executeTaskMsg.getVmId());
        this.tasks.put(task.getTaskId(), task);

        this.receivedData.computeIfAbsent(task.getCycle(), k -> new HashSet<>());

        var idToTrue = task.getParents().stream().map(id -> Pair.of(id, true)).collect(Collectors.toList());

        task.getTaskState().setEnterFogDeviceWaitingQueue(task.getCycle(), CloudSim.clock());

        if (this.receivedData.get(task.getCycle()).containsAll(idToTrue)) {
            log("All parent data for Task(%s) are received. task can execute", task.getTaskId());

            task.getTaskState().setStartExecutionTime(task.getCycle(), CloudSim.clock());
            sendNow(
                    getId(),
                    CloudSimTags.CLOUDLET_SUBMIT_ACK,
                    task
            );

            task.getTaskState().setExitFogDeviceWaitingQueue(task.getCycle(), CloudSim.clock());
            this.waitingTasks.removeIf(taskId -> taskId == task.getTaskId());
        } else if (this.receivedData.get(task.getCycle()).stream().anyMatch(pair -> task.getParents().contains(pair.getFirst()) && !pair.getSecond())) {
            log("One of parents of Task(%s) did not generate data on Cycle(%s). Sending back Task...", task.getTaskId(), task.getCycle());

            task.getCycleToGeneratedData().put(task.getCycle(), false);

            task.getTaskState().setStartExecutionTime(task.getCycle(), CloudSim.clock());
            sendNow(
                    task.getUserId(),
                    CloudSimTags.CLOUDLET_RETURN,
                    task
            );

            task.getTaskState().setExitFogDeviceWaitingQueue(task.getCycle(), CloudSim.clock());
            this.waitingTasks.removeIf(taskId -> taskId == task.getTaskId());
        } else {
            log("Not all parent data are available. task is added to waiting queue");
            task.getTaskState().setEnterFogDeviceWaitingQueue(task.getCycle(), CloudSim.clock());
            this.waitingTasks.add(task.getTaskId());
        }
    }

    protected void processExecuteTaskWithData(SimEvent event) {
        ExecuteTaskMsg executeTaskMsg = (ExecuteTaskMsg) event.getData();

        log("Received Task(%s) from Workflow(%s) with STAGE_IN data to run on Vm(%s)", executeTaskMsg.getTask().getTaskId(), executeTaskMsg.getTask().getWorkflowId(), executeTaskMsg.getVmId());

        double delay = (double) executeTaskMsg.getTask().getTotalInputDataSize() / this.downLinkBw;

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
        String tag = String.format("FogDevice(%s)", getId());

        Logger.log(tag, formatted, args);
    }

    @Override
    public String toString() {
        return String.format("FogDevice(%s)", getId());
    }
}
