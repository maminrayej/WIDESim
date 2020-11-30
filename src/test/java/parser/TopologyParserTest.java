package parser;

import misty.core.Enums;
import misty.entity.FogDevice;
import misty.entity.FogHost;
import misty.entity.FogVm;
import misty.parse.Default;
import misty.parse.topology.Parser;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.jgrapht.alg.util.Pair;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TopologyParserTest {

    @Nested
    public class FileTest {

        @Test
        @DisplayName("file does not exist")
        void topologyFileDoesNotExist() {
            assertThrows(IOException.class, () -> new Parser(new File("InvalidFile.json")));
        }

        @Test
        @DisplayName("path is a directory")
        void topologyFileIsDir() {
            assertThrows(IllegalArgumentException.class, () -> new Parser(new File("./")));
        }

//        @Test
//        @DisplayName("can not read file")
//        void canNotReadTopologyFile() {
//            assertThrows(IllegalAccessException.class, () -> {
//                new Parser(new File("src/test/resources/parser/can_not_read_file.txt"));
//            });
//        }

        @Test
        @DisplayName("read file successfully")
        void readFileSuccessfully() throws IOException, IllegalAccessException {
            Parser topologyParser = new Parser(new File("src/test/resources/parser/can_read_file.txt"));

            Object content = FieldUtils.readField(topologyParser, "topologyJsonContent", true);

            assertEquals("test content 1\ntest content 2", content.toString());
        }
    }

    @Nested
    public class FogDeviceTest {
        @Test
        @DisplayName("empty topology")
        void parseEmptyTopology() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/resources/parser/topology/empty.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no fog device id")
        void parseFileWithNoDeviceId() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/resources/parser/topology/no_device_id.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no neighbors array")
        void parseFileWithNoNeighborsList() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/resources/parser/topology/no_device_id.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no host id")
        void parseFileWithNoHostId() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/resources/parser/topology/no_host_id.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no vm id")
        void parseFileWithNoVmId() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/resources/parser/topology/no_vm_id.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no pe id")
        void parseFileWithNoPeId() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/resources/parser/topology/no_pe_id.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("minimal valid")
        void parseMinimalValidTopology() throws IOException, IllegalAccessException {
            CloudSim.init(1, Calendar.getInstance(), false);

            Parser parser = new Parser(new File("src/test/resources/parser/topology/minimal_valid.json"));

            Pair<List<FogDevice>, List<FogVm>> result = parser.parse();

            List<FogDevice> fogDevices = result.getFirst();
            List<FogVm> vms = result.getSecond();

            // all fog devices must pass the validation filter
            AtomicInteger fogDeviceNum = new AtomicInteger(-1);
            assertEquals(fogDevices.size(), fogDevices.stream().filter(fogDevice -> {
                fogDeviceNum.getAndIncrement();
                return isDefaultFogDeviceValid(fogDevice, "device_" + fogDeviceNum);
            }).count());

            // all vms must pass the filter
            AtomicInteger vmId = new AtomicInteger(-1);
            assertEquals(vms.size(), vms.stream().filter(vm -> {
                vmId.getAndIncrement();
                return isDefaultVmValid(vm, vmId.get());
            }).count());
        }

        @Test
        @DisplayName("valid")
        void parseValidTopology() throws IOException, IllegalAccessException {
            CloudSim.init(1, Calendar.getInstance(), false);

            Parser parser = new Parser(new File("src/test/resources/parser/topology/valid.json"));

            Pair<List<FogDevice>, List<FogVm>> result = parser.parse();

            List<FogDevice> fogDevices = result.getFirst();
            List<FogVm> vms = result.getSecond();

            // all fog devices must pass the validation filter
            AtomicInteger fogDeviceNum = new AtomicInteger(-1);
            assertEquals(fogDevices.size(), fogDevices.stream().filter(fogDevice -> {
                fogDeviceNum.getAndIncrement();
                return isDefaultFogDeviceValid(fogDevice, "device_" + fogDeviceNum);
            }).count());

            // all vms must pass the filter
            AtomicInteger vmId = new AtomicInteger(-1);
            assertEquals(vms.size(), vms.stream().filter(vm -> {
                vmId.getAndIncrement();
                return isDefaultVmValid(vm, vmId.get());
            }).count());
        }

        private boolean isDefaultFogDeviceValid(FogDevice fogDevice, String fogDeviceId) {
            try {
                // use reflection to get access to datacenterCharacteristics
                Method getDatacenterCharacteristics = Datacenter.class.getDeclaredMethod("getCharacteristics");
                getDatacenterCharacteristics.setAccessible(true);
                DatacenterCharacteristics datacenterCharacteristics = (DatacenterCharacteristics) getDatacenterCharacteristics.invoke(fogDevice);

                // extract datacenter characteristics
                String arch = FieldUtils.readDeclaredField(datacenterCharacteristics, "architecture", true).toString();
                String os = FieldUtils.readDeclaredField(datacenterCharacteristics, "os", true).toString();
                String vmm = datacenterCharacteristics.getVmm();
                double timeZone = (double) FieldUtils.readDeclaredField(datacenterCharacteristics, "timeZone", true);
                double costPerSec = datacenterCharacteristics.getCostPerSecond();
                double costPerMem = datacenterCharacteristics.getCostPerMem();
                double costPerStorage = datacenterCharacteristics.getCostPerStorage();
                double costPerBw = datacenterCharacteristics.getCostPerBw();
                var vmAllocationPolicyClass = fogDevice.getVmAllocationPolicy().getClass();
                double schedulingInterval = (double) FieldUtils.readField(fogDevice, "schedulingInterval", true);

                AtomicInteger hostId = new AtomicInteger(-1);
                boolean areHostsValid = fogDevice.getHosts().stream().allMatch(fogHost -> {
                    hostId.getAndIncrement();
                    return isDefaultHostValid(fogHost, hostId.get());
                });

                // make sure everything is set to default
                return fogDevice.getName().equals(fogDeviceId) &&
                        arch.equals(Default.FOG_DEVICE.ARCH.toString()) &&
                        os.equals(Default.FOG_DEVICE.OS.toString()) &&
                        vmm.equals(Default.FOG_DEVICE.VMM.toString()) &&
                        timeZone == Default.FOG_DEVICE.TIME_ZONE &&
                        costPerSec == Default.FOG_DEVICE.COST_PER_SEC &&
                        costPerMem == Default.FOG_DEVICE.COST_PER_MEM &&
                        costPerStorage == Default.FOG_DEVICE.COST_PER_STORAGE &&
                        costPerBw == Default.FOG_DEVICE.COST_PER_BW &&
                        vmAllocationPolicyClass == Enums.VmAllocPolicyEnum.getPolicy(Default.FOG_DEVICE.VM_ALLOC_POLICY.toString(), new ArrayList<>()).getClass() &&
                        schedulingInterval == Default.FOG_DEVICE.SCHEDULING_INTERVAL &&
                        areHostsValid;

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                return false;
            }
        }

        private boolean isDefaultHostValid(FogHost host, int hostId) {
            long storageCap = host.getStorage();
            int ram = host.getRam();
            long bw = host.getBw();
            var ramProvisioningClass = host.getRamProvisioner().getClass();
            var bwProvisioningClass = host.getBwProvisioner().getClass();
            var vmSchedulerClass = host.getVmScheduler().getClass();
//            var powerModelClass = host.getPowerModel().getClass();
            double maxPower;
            double staticPower;

//            try {
//                maxPower = (double) FieldUtils.readField(host.getPowerModel(), "maxPower", true);
//                staticPower = (double) FieldUtils.readField(host.getPowerModel(), "staticPower", true);
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//                return false;
//            }

            AtomicInteger peId = new AtomicInteger(-1);
            boolean arePesValid = host.getPeList().stream().allMatch(pe -> {
                peId.getAndIncrement();
                return isDefaultPeValid(pe, peId.get());
            });

            return host.getId() == hostId &&
                    storageCap == Default.HOST.STORAGE_CAP &&
                    ram == Default.HOST.RAM &&
                    bw == Default.HOST.BW &&
                    ramProvisioningClass == Enums.RamProvisionerEnum.getProvisioner(Default.HOST.RAM_PROVISIONER.toString(), 1).getClass() &&
                    bwProvisioningClass == Enums.BwProvisionerEnum.getProvisioner(Default.HOST.BW_PROVISIONER.toString(), 1).getClass() &&
                    vmSchedulerClass == Enums.VmSchedulerEnum.getScheduler(Default.HOST.VM_SCHEDULER.toString(), new ArrayList<>()).getClass() &&
//                    powerModelClass == Enums.PowerModelEnum.getPowerModel(Default.HOST.POWER_MODEL.toString(), 2, 1).getClass() &&
//                    maxPower == Default.HOST.MAX_POWER &&
//                    staticPower / maxPower == Default.HOST.STATIC_POWER_PERCENT &&
                    arePesValid;
        }

        private boolean isDefaultPeValid(Pe pe, int peId) {
            return pe.getId() == peId &&
                    pe.getMips() == Default.PE.MIPS &&
                    pe.getPeProvisioner().getClass() == Enums.PeProvisionerEnum.getProvisioner(Default.PE.PE_PROVISIONING.toString(), 1).getClass();
        }

        private boolean isDefaultVmValid(Vm vm, int vmId) {
            return vm.getId() == vmId &&
                    vm.getSize() == Default.VM.SIZE &&
                    vm.getMips() == Default.VM.MIPS &&
                    vm.getNumberOfPes() == Default.VM.NUM_OF_PES &&
                    vm.getRam() == Default.VM.RAM &&
                    vm.getBw() == Default.VM.BW &&
                    vm.getVmm().equals(Default.VM.VMM.toString()) &&
                    vm.getCloudletScheduler().getClass() == Enums.CloudletSchedulerEnum.getScheduler(Default.VM.CLOUDLET_SCHEDULER.toString(), 1, 1).getClass();
        }
    }
}
