package misty.entity;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
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
