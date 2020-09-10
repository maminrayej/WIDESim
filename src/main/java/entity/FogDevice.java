package entity;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.power.PowerDatacenter;

import java.util.HashMap;
import java.util.List;

public class FogDevice extends PowerDatacenter {

    private HashMap<String, String> routingTable;
    private List<String> neighbors;

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

    public void addToRoutingTable(String destination, String hop) {
        this.routingTable.put(destination, hop);
    }
}
