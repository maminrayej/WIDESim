package widesim.entity;

import widesim.computation.Task;
import widesim.core.Constants;
import widesim.core.Logger;
import widesim.message.*;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerVm;
import org.jgrapht.alg.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class FogDevice extends PowerDatacenter {
    private boolean isUpLinkBusy;
    private boolean isDownLinkBusy;

    private final ArrayDeque<NetworkRequest> downLinkQueue;
    private final ArrayDeque<NetworkRequest> upLinkQueue;

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

        this.isUpLinkBusy = false;
        this.isDownLinkBusy = false;

        this.upLinkQueue = new ArrayDeque<>();
        this.downLinkQueue = new ArrayDeque<>();
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
            case Constants.MsgTag.DOWN_LINK_IS_FREE -> processDownLinkIsFree(event);
            case Constants.MsgTag.UP_LINK_IS_FREE -> processUpLinkIsFree(event);
            default -> super.processEvent(event);
        }
    }

    protected void processDownLinkIsFree(SimEvent event) {
        NetworkRequest request = downLinkQueue.poll();

        if (request != null) {
            send(
                    request.dstEntityId,
                    request.delay,
                    request.tag,
                    request.msg
            );

            send(
                    getId(),
                    request.delay,
                    Constants.MsgTag.DOWN_LINK_IS_FREE
            );
        } else {
            isDownLinkBusy = false;
        }
    }

    protected void processUpLinkIsFree(SimEvent event) {
        NetworkRequest request = upLinkQueue.poll();

        if (request != null) {
            send(
                    request.dstEntityId,
                    request.delay,
                    request.tag,
                    request.msg
            );

            send(
                    getId(),
                    request.delay,
                    Constants.MsgTag.UP_LINK_IS_FREE
            );
        } else {
            isUpLinkBusy = false;
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

        PowerVm vm = vmCreateMsg.getVm();

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

        Task task = this.tasks.get(stageOutDataMsg.getTaskId());

        if (stageOutDataMsg.getNeededFiles() == null) {
            System.out.println(">>>> Needed files is empty! for task: " + task.getTaskId());
        }
        long aggregatedOutputSize = 0;
        if (stageOutDataMsg.getNeededFiles() != null) {
            aggregatedOutputSize = stageOutDataMsg.getNeededFiles().stream().mapToLong(task::getFileSize).sum();
        }

        // If destination is this fog device
        if (stageOutDataMsg.getDstFogDeviceId() == this.getId()) {
            log("Received STAGE_OUT data of Task(%s) on Cycle(%s): (%s) and destination is a loop back: sending data instantly", stageOutDataMsg.getTaskId(),
                    stageOutDataMsg.getCycle(), stageOutDataMsg.isData());
            sendNow(
                    getId(),
                    Constants.MsgTag.DOWNLOADED_FOG_TO_FOG,
                    new FogToFogMsg(
                            getId(),
                            stageOutDataMsg.getTaskId(),
                            stageOutDataMsg.getCycle(),
                            aggregatedOutputSize,
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

        log("Sending data of Task(%s) on Cycle(%s) to FogDevice(%s) with Delay(%s)", stageOutDataMsg.getTaskId(), this.tasks.get(stageOutDataMsg.getTaskId()).getCycle(), nextHopId, stageOutDataMsg.isData() ? (double) aggregatedOutputSize / this.upLinkBw : 0);
        double delay = stageOutDataMsg.isData() ? (double) aggregatedOutputSize / this.upLinkBw : 0;
        FogToFogMsg msg = new FogToFogMsg(
                stageOutDataMsg.getDstFogDeviceId(),
                stageOutDataMsg.getTaskId(),
                stageOutDataMsg.getCycle(),
                task.getCloudletOutputSize(),
                stageOutDataMsg.isData()
        );

        if (isUpLinkBusy) {
            upLinkQueue.add(new NetworkRequest(
                    nextHopId,
                    delay,
                    Constants.MsgTag.FOG_TO_FOG,
                    msg
            ));
        } else {
            isUpLinkBusy = true;

            // Upload the data
            send(
                    nextHopId,
                    delay,
                    Constants.MsgTag.FOG_TO_FOG,
                    msg
            );

            // Notify that uplink will be free after `delay` time has passed
            send(
                    getId(),
                    delay,
                    Constants.MsgTag.UP_LINK_IS_FREE
            );
        }
    }

    protected void processFogToFog(SimEvent event) {
        FogToFogMsg fogToFogMsg = (FogToFogMsg) event.getData();

        log("Downloading data from fog device: %s", event.getSource());

        double delay = fogToFogMsg.isData() ? (double) fogToFogMsg.getData() / this.downLinkBw : 0;
        if (isDownLinkBusy) {
            downLinkQueue.add(new NetworkRequest(
               getId(),
               delay,
               Constants.MsgTag.DOWNLOADED_FOG_TO_FOG,
               fogToFogMsg
            ));
        } else {
            isDownLinkBusy = true;

            // Download the data
            send(
                    getId(),
                    delay,
                    Constants.MsgTag.DOWNLOADED_FOG_TO_FOG,
                    fogToFogMsg
            );

            // Notify that download link will be free after `delay` time has passed
            send(
                    getId(),
                    delay,
                    Constants.MsgTag.DOWN_LINK_IS_FREE
            );
        }
    }

    protected void processDownloadedFogToFog(SimEvent event) {
        FogToFogMsg fogToFogMsg = (FogToFogMsg) event.getData();
        log("Downloaded data of Task(%s) on Cycle(%s)", fogToFogMsg.getTaskId(), fogToFogMsg.getCycle());

        if (this.getId() == fogToFogMsg.getDstFogDeviceId()) {
            log("Data is for me");

            this.receivedData.computeIfAbsent(fogToFogMsg.getCycle(), k -> new HashSet<>());

            log("Updating received data with: Cycle(%s): Task(%s) -> %s", fogToFogMsg.getCycle(), fogToFogMsg.getTaskId(), fogToFogMsg.isData());
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
                    it.remove();
                }
            }
        } else {
            log("Data is not for me. relaying the message");

            String dstName = this.idToName.get(fogToFogMsg.getDstFogDeviceId());

            String nextHopName = this.nextHop(dstName);

            int nextHopId = this.nameToId.get(nextHopName);

            double delay = (double) fogToFogMsg.getData() / this.upLinkBw;

            if (isUpLinkBusy) {
                upLinkQueue.add(new NetworkRequest(
                   nextHopId,
                   delay,
                   Constants.MsgTag.FOG_TO_FOG,
                   fogToFogMsg
                ));
            } else {
                isUpLinkBusy = true;

                send(
                        nextHopId,
                        delay,
                        Constants.MsgTag.FOG_TO_FOG,
                        fogToFogMsg
                );

                send(
                        getId(),
                        delay,
                        Constants.MsgTag.UP_LINK_IS_FREE
                );
            }
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

        double maxRate = Double.MIN_VALUE;
        for (Storage storage : getStorageList()) {
            double rate = storage.getMaxTransferRate();
            if (rate > maxRate) {
                maxRate = rate;
            }
        }

//        double delay = ((double) executeTaskMsg.getTask().getTotalInputDataSize() / this.downLinkBw) + 0;
        double delay = 0.0;
        if (isDownLinkBusy) {
            downLinkQueue.add(new NetworkRequest(
                    getId(),
                    delay,
                    Constants.MsgTag.EXECUTE_TASK,
                    executeTaskMsg
            ));
        } else {
            isDownLinkBusy = true;

            // Download the data
            send(
                    getId(),
                    delay,
                    Constants.MsgTag.EXECUTE_TASK,
                    executeTaskMsg
            );

            // Notify that download link will be free after `delay` time has passed
            send(
                    getId(),
                    delay,
                    Constants.MsgTag.DOWN_LINK_IS_FREE
            );
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

    private void log(String formatted, Object... args) {
        String tag = String.format("FogDevice(%s|%s)", getId(), getName());

        Logger.log(tag, formatted, args);
    }

    @Override
    public String toString() {
        return String.format("FogDevice(%s)", getId());
    }

    @Override
    protected void updateCloudletProcessing() {
        // if some time passed since last processing
        // R: for term is to allow loop at simulation start. Otherwise, one initial
        // simulation step is skipped and schedulers are not properly initialized
        //this is a bug of CloudSim if the runtime is smaller than 0.1 (now is 0.01) it doesn't work at all
        if (CloudSim.clock() < 0.111 || CloudSim.clock() > getLastProcessTime() + 0.01) {
            List<? extends Host> list = getVmAllocationPolicy().getHostList();
            double smallerTime = Double.MAX_VALUE;
            // for each host...
            for (Host host : list) {
                // inform VMs to update processing
                double time = host.updateVmsProcessing(CloudSim.clock());
                // what time do we expect that the next cloudlet will finish?
                if (time < smallerTime) {
                    smallerTime = time;
                }
            }
            // gurantees a minimal interval before scheduling the event
            if (smallerTime < CloudSim.clock() + 0.11) {
                smallerTime = CloudSim.clock() + 0.11;
            }
            if (smallerTime != Double.MAX_VALUE) {
                schedule(getId(), (smallerTime - CloudSim.clock()), CloudSimTags.VM_DATACENTER_EVENT);
            }
            setLastProcessTime(CloudSim.clock());
        }
    }

    @Override
    protected void checkCloudletCompletion() {
        List<? extends Host> list = getVmAllocationPolicy().getHostList();
        for (Host host : list) {
            for (Vm vm : host.getVmList()) {
                while (vm.getCloudletScheduler().isFinishedCloudlets()) {
                    Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
                    if (cl != null) {
                        sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                    }
                }
            }
        }
    }
}

class NetworkRequest {
    int dstEntityId;
    double delay;
    int tag;
    Object msg;

    public NetworkRequest(int dstEntityId, double delay, int tag, Object msg) {
        this.dstEntityId = dstEntityId;
        this.delay = delay;
        this.tag = tag;
        this.msg = msg;
    }
}
