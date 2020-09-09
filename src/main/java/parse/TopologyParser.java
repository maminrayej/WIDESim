package parse;

import core.Constants;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static parse.Helper.getOr;

public class TopologyParser {

    public static void parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);

        // Parse fog devices
        List<Datacenter> fogDevices = root.getJSONArray("fog_devices").toList().stream().map(fogDevice -> {
            // Parse fog device attributes
            JSONObject fogDeviceObj = (JSONObject) fogDevice;
            String deviceId = fogDeviceObj.getString("device_id");
            String arch = getOr(fogDeviceObj, "architecture", "x86_64", String.class);
            String os = getOr(fogDeviceObj, "os", "Linux", String.class);
            List<String> neighborIds = fogDeviceObj
                    .getJSONArray("neighbors")
                    .toList()
                    .stream()
                    .map(obj -> ((JSONObject) obj).getString("fog_device_id"))
                    .collect(Collectors.toList());
            double timeZone = getOr(fogDeviceObj, "time_zone", 0.0, double.class);
            double costPerSec = getOr(fogDeviceObj, "cost_per_sec", 1.0, double.class);
            double costPerStorage = getOr(fogDeviceObj, "cost_per_storage", 1.0, double.class);
            double costPerMemory = getOr(fogDeviceObj, "cost_per_memory", 1.0, double.class);
            double costPerBw = getOr(fogDeviceObj, "cost_per_bw", 1.0, double.class);
            String vmAllocationPolicy = getOr(fogDeviceObj, "vm_allocation_policy", "Simple", String.class);
            String allocationPolicy = getOr(fogDeviceObj, "allocation_policy", "TimeShared", String.class);
            String vmm = getOr(fogDeviceObj, "vmm", "Xen", String.class);
            double schedulingInterval = getOr(fogDeviceObj, "schedule_interval", 0.0, double.class);

            // Parse hosts
            List<Host> hosts = fogDeviceObj.getJSONArray("hosts").toList().stream().map(host -> {
                // Parse host attributes
                JSONObject hostObj = (JSONObject) host;
                int hostId = hostObj.getInt("host_id");
                Long storageCap = getOr(hostObj, "storage_cap", Constants.DataUnit.GB, Long.class);
                int ram = getOr(hostObj, "ram", Constants.PowOfTwo.NINE, int.class);
                long bw = getOr(hostObj, "bw", (long) Constants.PowOfTwo.TEN, long.class);
                String ramProvisioning = getOr(hostObj, "ram_provisioning", "Simple", String.class);
                String bwProvisioning = getOr(hostObj, "bw_provisioning", "Simple", String.class);
                String vmScheduler = getOr(hostObj, "vm_scheduler", "TimeShared", String.class);

                // Parse vms
                List<Vm> vms = hostObj.getJSONArray("vms").toList().stream().map(vm -> {
                    // Parse vm attributes
                    JSONObject vmObj = (JSONObject) vm;
                    int vmId = vmObj.getInt("vm_id");
                    Long size = getOr(vmObj, "size", Constants.DataUnit.MB, Long.class);
                    double mips = getOr(vmObj, "mips", (double) Constants.MetricUnit.GIGA, double.class);
                    int numOfPes = getOr(vmObj, "num_of_pes", 1, int.class);
                    int vmRam = getOr(vmObj, "ram", Constants.PowOfTwo.NINE, int.class);
                    long vmBw = getOr(vmObj, "bw", (long) Constants.PowOfTwo.TEN, long.class);
                    String vmVmm = getOr(vmObj, "vmm", "Xen", String.class);
                    String cloudletScheduler = getOr(vmObj, "cloudlet_scheduler", "SpaceShared", String.class);
                    return new Vm(vmId, hostId, mips, numOfPes, vmRam, vmBw, size, vmVmm, new CloudletSchedulerSpaceShared());
                }).collect(Collectors.toList());

                // Parse pes
                List<Pe> pes = hostObj.getJSONArray("pes").toList().stream().map(pe -> {
                    // Parse pe attributes
                    JSONObject peObj = (JSONObject) pe;
                    int peId = peObj.getInt("pe_id");
                    double mips = getOr(peObj, "mips", (double) Constants.PowOfTwo.TEN, double.class);
                    String peProvisioning = getOr(peObj, "pe_provisioning", "Simple", String.class);
                    return new Pe(peId, new PeProvisionerSimple(mips));
                }).collect(Collectors.toList());

                return new Host(hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storageCap, pes, new VmSchedulerTimeShared(pes));
            }).collect(Collectors.toList());


            DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hosts, timeZone, costPerSec, costPerMemory, costPerStorage, costPerBw);

            try {
                return new Datacenter(deviceId, characteristics, new VmAllocationPolicySimple(hosts), new LinkedList<>(), schedulingInterval);
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage());
            }
        }).collect(Collectors.toList());
    }
}
