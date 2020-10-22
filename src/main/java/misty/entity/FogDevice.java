package misty.entity;

import misty.core.Constants;
import misty.message.ResourceRequestResponseMsg;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;

import java.util.HashMap;
import java.util.List;

public class FogDevice extends PowerDatacenter {

    private final HashMap<String, String> routingTable;

    private final List<String> neighbors;

    public FogDevice(String name,
                     DatacenterCharacteristics characteristics,
                     VmAllocationPolicy vmAllocationPolicy,
                     List<Storage> storageList,
                     double schedulingInterval,
                     List<String> neighbors) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);

        this.neighbors = neighbors;
        this.routingTable = new HashMap<>();
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
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            // request each fog device for its characteristics
            case Constants.MsgTag.RESOURCE_REQUEST:
                processResourceRequest(ev);
                break;
        }
    }

    protected void processResourceRequest(SimEvent event) {
        sendNow(event.getSource(), Constants.MsgTag.RESOURCE_REQUEST_RESPONSE, new ResourceRequestResponseMsg(this.getCharacteristics()));
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
