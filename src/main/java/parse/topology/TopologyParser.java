package parse.topology;

import core.Enums.*;
import entity.FogDevice;
import entity.FogHost;
import org.cloudbus.cloudsim.*;
import org.json.JSONException;
import org.json.JSONObject;
import parse.Default;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static parse.Helper.getOrDefault;

public class TopologyParser {

    public static List<FogDevice> parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);

        // Parse fog devices
        return root.getJSONArray(Tags.FogDevice.FOG_DEVICES).toList().stream().map(fogDevice -> {
            // Parse fog device attributes
            JSONObject fogDeviceObj = (JSONObject) fogDevice;
            String deviceId = fogDeviceObj.getString(Tags.FogDevice.DEVICE_ID);
            String arch = getOrDefault(fogDeviceObj, Tags.FogDevice.ARCH, Default.FOG_DEVICE.ARCH.toString(), String.class);
            String os = getOrDefault(fogDeviceObj, Tags.FogDevice.OS, Default.FOG_DEVICE.OS.toString(), String.class);
            List<String> neighborIds = fogDeviceObj
                    .getJSONArray(Tags.FogDevice.NEIGHBORS)
                    .toList()
                    .stream()
                    .map(obj -> ((JSONObject) obj).getString(Tags.FogDevice.NEIGHBOR_ID))
                    .collect(Collectors.toList());
            double timeZone = getOrDefault(fogDeviceObj, Tags.FogDevice.TIME_ZONE, Default.FOG_DEVICE.TIME_ZONE, double.class);
            double costPerSec = getOrDefault(fogDeviceObj, Tags.FogDevice.COST_PER_SEC, Default.FOG_DEVICE.COST_PER_SEC, double.class);
            double costPerStorage = getOrDefault(fogDeviceObj, Tags.FogDevice.COST_PER_STORAGE, Default.FOG_DEVICE.COST_PER_STORAGE, double.class);
            double costPerMemory = getOrDefault(fogDeviceObj, Tags.FogDevice.COST_PER_MEM, Default.FOG_DEVICE.COST_PER_MEM, double.class);
            double costPerBw = getOrDefault(fogDeviceObj, Tags.FogDevice.COST_PER_BW, Default.FOG_DEVICE.COST_PER_BW, double.class);
            String vmAllocationPolicy = getOrDefault(fogDeviceObj, Tags.FogDevice.VM_ALLOC_POLICY, Default.FOG_DEVICE.VM_ALLOC_POLICY.toString(), String.class);
            String vmm = getOrDefault(fogDeviceObj, Tags.FogDevice.VMM, Default.FOG_DEVICE.VMM.toString(), String.class);
            double schedulingInterval = getOrDefault(fogDeviceObj, Tags.FogDevice.SCHEDULE_INTERVAL, Default.FOG_DEVICE.SCHEDULING_INTERVAL, double.class);

            // Parse hosts
            List<FogHost> hosts = fogDeviceObj.getJSONArray(Tags.FogDevice.HOSTS).toList().stream().map(host -> {
                // Parse host attributes
                JSONObject hostObj = (JSONObject) host;
                int hostId = hostObj.getInt(Tags.Host.HOST_ID);
                long storageCap = getOrDefault(hostObj, Tags.Host.STORAGE_CAP, Default.HOST.STORAGE_CAP, Long.class);
                int ram = getOrDefault(hostObj, Tags.Host.RAM, Default.HOST.RAM, int.class);
                long bw = getOrDefault(hostObj, Tags.Host.BW, Default.HOST.BW, long.class);
                String ramProvisioning = getOrDefault(hostObj, Tags.Host.RAM_PROV, Default.HOST.RAM_PROVISIONER.toString(), String.class);
                String bwProvisioning = getOrDefault(hostObj, Tags.Host.BW_PROV, Default.HOST.BW_PROVISIONER.toString(), String.class);
                String vmScheduler = getOrDefault(hostObj, Tags.Host.VM_SCHEDULER, Default.HOST.VM_SCHEDULER.toString(), String.class);
                String powerModel = getOrDefault(hostObj, Tags.Host.POWER_MODEL, Default.HOST.POWER_MODEL.toString(), String.class);
                double maxPower = getOrDefault(hostObj, Tags.Host.MAX_POWER, Default.HOST.MAX_POWER, double.class);
                double idlePower = getOrDefault(hostObj, Tags.Host.IDLE_POWER, Default.HOST.IDLE_POWER, double.class);

                // Parse vms
                List<Vm> vms = hostObj.getJSONArray(Tags.Host.VMS).toList().stream().map(vm -> {
                    // Parse vm attributes
                    JSONObject vmObj = (JSONObject) vm;
                    int vmId = vmObj.getInt(Tags.Vm.VM_ID);
                    long size = getOrDefault(vmObj, Tags.Vm.SIZE, Default.VM.SIZE, Long.class);
                    double mips = getOrDefault(vmObj, Tags.Vm.MIPS, Default.VM.MIPS, double.class);
                    int numOfPes = getOrDefault(vmObj, Tags.Vm.NUM_OF_PES, Default.VM.NUM_OF_PES, int.class);
                    int vmRam = getOrDefault(vmObj, Tags.Vm.RAM, Default.VM.RAM, int.class);
                    long vmBw = getOrDefault(vmObj, Tags.Vm.BW, Default.VM.BW, long.class);
                    String vmVmm = getOrDefault(vmObj, Tags.Vm.VMM, Default.VM.VMM.toString(), String.class);
                    String cloudletScheduler = getOrDefault(vmObj, Tags.Vm.CLOUDLET_SCHEDULER, Default.VM.CLOUDLET_SCHEDULER.toString(), String.class);

                    return new Vm(
                            vmId, hostId, mips, numOfPes, vmRam, vmBw, size, vmVmm,
                            CloudletSchedulerEnum.getScheduler(cloudletScheduler, mips, numOfPes)
                    );
                }).collect(Collectors.toList());

                // Parse pes
                List<Pe> pes = hostObj.getJSONArray(Tags.Host.PES).toList().stream().map(pe -> {
                    // Parse pe attributes
                    JSONObject peObj = (JSONObject) pe;
                    int peId = peObj.getInt(Tags.Pe.PE_ID);
                    double mips = getOrDefault(peObj, Tags.Pe.MIPS, Default.PE.MIPS, double.class);
                    String peProvisioning = getOrDefault(peObj, Tags.Pe.PE_PROVISIONING, Default.PE.PE_PROVISIONING.toString(), String.class);

                    return new Pe(
                            peId,
                            PeProvisionerEnum.getProvisioner(peProvisioning, mips)
                    );
                }).collect(Collectors.toList());

                return new FogHost(
                        hostId,
                        RamProvisionerEnum.getProvisioner(ramProvisioning, ram),
                        BwProvisionerEnum.getProvisioner(bwProvisioning, bw),
                        storageCap,
                        pes,
                        VmSchedulerEnum.getScheduler(vmScheduler, pes),
                        PowerModelEnum.getPowerModel(powerModel, maxPower, idlePower),
                        vms
                );
            }).collect(Collectors.toList());

            DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                    arch, os, vmm, hosts, timeZone, costPerSec,
                    costPerMemory, costPerStorage, costPerBw
            );

            try {
                return new FogDevice(
                        deviceId,
                        characteristics,
                        VmAllocPolicyEnum.getPolicy(vmAllocationPolicy, hosts),
                        new LinkedList<>(),
                        schedulingInterval,
                        neighborIds
                );
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }).collect(Collectors.toList());
    }

    private static class Tags {
        public static class FogDevice {
            public static final String FOG_DEVICES = "fog_devices";
            public static final String DEVICE_ID = "device_id";
            public static final String ARCH = "architecture";
            public static final String OS = "os";
            public static final String NEIGHBORS = "neighbors";
            public static final String NEIGHBOR_ID = "neighbor_id";
            public static final String TIME_ZONE = "time_zone";
            public static final String COST_PER_SEC = "cost_per_sec";
            public static final String COST_PER_STORAGE = "cost_per_storage";
            public static final String COST_PER_MEM = "cost_per_mem";
            public static final String COST_PER_BW = "cost_per_bw";
            public static final String VM_ALLOC_POLICY = "vm_alloc_policy";
            public static final String VMM = "vmm";
            public static final String SCHEDULE_INTERVAL = "schedule_interval";
            public static final String HOSTS = "hosts";
        }

        public static class Host {
            public static final String HOST_ID = "host_id";
            public static final String STORAGE_CAP = "storage_cap";
            public static final String RAM = "ram";
            public static final String BW = "bw";
            public static final String RAM_PROV = "ram_provisioning";
            public static final String BW_PROV = "bw_provisioning";
            public static final String VM_SCHEDULER = "vm_scheduler";
            public static final String POWER_MODEL = "power_model";
            public static final String MAX_POWER = "max_power";
            public static final String IDLE_POWER = "idle_power";
            public static final String VMS = "vms";
            public static final String PES = "pes";
        }

        public static class Vm {
            public static final String VM_ID = "vm_id";
            public static final String SIZE = "size";
            public static final String MIPS = "mips";
            public static final String NUM_OF_PES = "num_of_pes";
            public static final String RAM = "ram";
            public static final String BW = "bw";
            public static final String VMM = "vmm";
            public static final String CLOUDLET_SCHEDULER = "cloudlet_scheduler";
        }

        public static class Pe {
            public static final String PE_ID = "pe_id";
            public static final String MIPS = "mips";
            public static final String PE_PROVISIONING = "pe_provisioning";
        }
    }
}