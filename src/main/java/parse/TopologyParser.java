package parse;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.provisioners.*;
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
            String arch = getOr(fogDeviceObj, "architecture", Default.FOG_DEVICE.ARCH, String.class);
            String os = getOr(fogDeviceObj, "os", Default.FOG_DEVICE.OS, String.class);
            List<String> neighborIds = fogDeviceObj
                    .getJSONArray("neighbors")
                    .toList()
                    .stream()
                    .map(obj -> ((JSONObject) obj).getString("fog_device_id"))
                    .collect(Collectors.toList());
            double timeZone = getOr(fogDeviceObj, "time_zone", Default.FOG_DEVICE.TIME_ZONE, double.class);
            double costPerSec = getOr(fogDeviceObj, "cost_per_sec", Default.FOG_DEVICE.COST_PER_SEC, double.class);
            double costPerStorage = getOr(fogDeviceObj, "cost_per_storage", Default.FOG_DEVICE.COST_PER_STORAGE, double.class);
            double costPerMemory = getOr(fogDeviceObj, "cost_per_memory", Default.FOG_DEVICE.COST_PER_MEM, double.class);
            double costPerBw = getOr(fogDeviceObj, "cost_per_bw", Default.FOG_DEVICE.COST_PER_BW, double.class);
            String vmAllocationPolicy = getOr(fogDeviceObj, "vm_allocation_policy", Default.FOG_DEVICE.VM_ALLOC_POLICY, String.class);
            String vmm = getOr(fogDeviceObj, "vmm", Default.FOG_DEVICE.VMM, String.class);
            double schedulingInterval = getOr(fogDeviceObj, "schedule_interval", Default.FOG_DEVICE.SCHEDULING_INTERVAL, double.class);

            // Parse hosts
            List<Host> hosts = fogDeviceObj.getJSONArray("hosts").toList().stream().map(host -> {
                // Parse host attributes
                JSONObject hostObj = (JSONObject) host;
                int hostId = hostObj.getInt("host_id");
                long storageCap = getOr(hostObj, "storage_cap", Default.HOST.STORAGE_CAP, Long.class);
                int ram = getOr(hostObj, "ram", Default.HOST.RAM, int.class);
                long bw = getOr(hostObj, "bw", Default.HOST.BW, long.class);
                String ramProvisioning = getOr(hostObj, "ram_provisioning", Default.HOST.RAM_PROVISIONER, String.class);
                String bwProvisioning = getOr(hostObj, "bw_provisioning", Default.HOST.BW_PROVISIONER, String.class);
                String vmScheduler = getOr(hostObj, "vm_scheduler", Default.HOST.VM_SCHEDULER, String.class);

                // Parse vms
                List<Vm> vms = hostObj.getJSONArray("vms").toList().stream().map(vm -> {
                    // Parse vm attributes
                    JSONObject vmObj = (JSONObject) vm;
                    int vmId = vmObj.getInt("vm_id");
                    long size = getOr(vmObj, "size", Default.VM.SIZE, Long.class);
                    double mips = getOr(vmObj, "mips", Default.VM.MIPS, double.class);
                    int numOfPes = getOr(vmObj, "num_of_pes", Default.VM.NUM_OF_PES, int.class);
                    int vmRam = getOr(vmObj, "ram", Default.VM.RAM, int.class);
                    long vmBw = getOr(vmObj, "bw", Default.VM.BW, long.class);
                    String vmVmm = getOr(vmObj, "vmm", Default.VM.VMM, String.class);
                    String cloudletScheduler = getOr(vmObj, "cloudlet_scheduler", "Space Shared", String.class);
                    return new Vm(vmId, hostId, mips, numOfPes, vmRam, vmBw, size, vmVmm, createCloudletScheduler(cloudletScheduler));
                }).collect(Collectors.toList());

                // Parse pes
                List<Pe> pes = hostObj.getJSONArray("pes").toList().stream().map(pe -> {
                    // Parse pe attributes
                    JSONObject peObj = (JSONObject) pe;
                    int peId = peObj.getInt("pe_id");
                    double mips = getOr(peObj, "mips", Default.PE.MIPS, double.class);
                    String peProvisioning = getOr(peObj, "pe_provisioning", Default.PE.PE_PROVISIONING, String.class);
                    return new Pe(peId, createPeProvisioner(peProvisioning, mips));
                }).collect(Collectors.toList());

                return new Host(
                        hostId,
                        createRamProvisioner(ramProvisioning, ram),
                        createBwProvisioner(bwProvisioning, bw),
                        storageCap,
                        pes,
                        createVmScheduler(vmScheduler, pes)
                );

            }).collect(Collectors.toList());


            DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hosts, timeZone, costPerSec, costPerMemory, costPerStorage, costPerBw);

            try {
                return new Datacenter(deviceId, characteristics, createVmAllocationPolicy(vmAllocationPolicy, hosts), new LinkedList<>(), schedulingInterval);
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage());
            }
        }).collect(Collectors.toList());
    }

    private static CloudletScheduler createCloudletScheduler(String cloudletSchedulerType) {
        switch (cloudletSchedulerType) {
            case "Space Shared":
                return new CloudletSchedulerSpaceShared();
            case "Time Shared":
                return new CloudletSchedulerTimeShared();
            default:
                return Default.VM.cloudletScheduler();
        }
    }

    private static RamProvisioner createRamProvisioner(String ramProvisionerType, int ram) {
        switch (ramProvisionerType) {
            case "Simple":
                return new RamProvisionerSimple(ram);
            default:
                return Default.HOST.ramProvisioner(ram);
        }
    }

    private static BwProvisioner createBwProvisioner(String bwProvisionerType, long bw) {
        switch (bwProvisionerType) {
            case "Simple":
                return new BwProvisionerSimple(bw);
            default:
                return Default.HOST.bwProvisioner(bw);
        }
    }

    private static VmScheduler createVmScheduler(String vmSchedulerType, List<Pe> pes) {
        switch (vmSchedulerType) {
            case "Time Shared":
                return new VmSchedulerTimeShared(pes);
            case "Space Shared":
                return new VmSchedulerSpaceShared(pes);
            default:
                return Default.HOST.vmScheduler(pes);
        }
    }

    private static PeProvisioner createPeProvisioner(String peProvisionerType, double mips) {
        switch (peProvisionerType) {
            case "Simple":
                return new PeProvisionerSimple(mips);
            default:
                return Default.PE.peProvisioner(mips);
        }
    }

    private static VmAllocationPolicy createVmAllocationPolicy(String vmAllocationPolicyType, List<Host> hosts) {
        switch (vmAllocationPolicyType) {
            case "Simple":
                return new VmAllocationPolicySimple(hosts);
            default:
                return Default.FOG_DEVICE.vmAllocationPolicy(hosts);
        }
    }
}
