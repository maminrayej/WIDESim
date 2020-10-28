package entity;

import misty.computation.Task;
import misty.computation.Workflow;
import misty.core.Enums;
import misty.entity.FogBroker;
import misty.entity.FogDevice;
import misty.entity.FogHost;
import misty.entity.TaskManager;
import misty.mapper.SimpleTaskToVmMapper;
import misty.mapper.SimpleVmToFogDeviceMapper;
import misty.parse.Default;
import misty.provision.SimpleVmProvisioner;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class FogBrokerTest {

    private FogDevice getDummyFogDevice(int deviceId) {
        List<FogHost> hostList = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            List<Pe> peList1 = new ArrayList<>();
            int mips = 2000;
            peList1.add(new Pe(0, new PeProvisionerSimple(mips)));
            peList1.add(new Pe(1, new PeProvisionerSimple(mips)));

            int hostId = 0;
            int ram = 2048;
            long storage = 1000000;
            int bw = 10000;
            hostList.add(
                    new FogHost(
                            hostId,
                            new RamProvisionerSimple(ram),
                            new BwProvisionerSimple(bw),
                            storage,
                            peList1,
                            new VmSchedulerTimeShared(peList1),
                            Enums.PowerModelEnum.getPowerModel(Default.HOST.POWER_MODEL.toString(), Default.HOST.MAX_POWER, Default.HOST.STATIC_POWER_PERCENT)
                            ));
        }

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                Default.FOG_DEVICE.ARCH.toString(), Default.FOG_DEVICE.OS.toString(),
                Default.FOG_DEVICE.VMM.toString(), hostList, Default.FOG_DEVICE.TIME_ZONE,
                Default.FOG_DEVICE.COST_PER_SEC, Default.FOG_DEVICE.COST_PER_MEM,
                Default.FOG_DEVICE.COST_PER_STORAGE, Default.FOG_DEVICE.COST_PER_BW
        );

        try {
            return new FogDevice(
                    String.valueOf(deviceId),
                    characteristics,
                    Enums.VmAllocPolicyEnum.getPolicy(Default.FOG_DEVICE.VM_ALLOC_POLICY.toString(), hostList),
                    new LinkedList<>(),
                    Default.FOG_DEVICE.SCHEDULING_INTERVAL,
                    null,
                    Default.FOG_DEVICE.UP_LINK_BW,
                    Default.FOG_DEVICE.DOWN_LINK_BW
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Test
    void testStarting() throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);

        // Tasks: 0 -> 1
        List<Task> tasks = new ArrayList<>(10) {{
            var children = new ArrayList<>(List.of(1));
            add(new Task(0, 1, 1, 1, 1,
                    null, null, null,
                    null, children, 0.0, 3.0, null));

            add(new Task(1, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 2.0, null));
        }};

        FogBroker broker = new FogBroker("broker", new SimpleVmProvisioner(), new SimpleVmToFogDeviceMapper(), new SimpleTaskToVmMapper());

        TaskManager taskManager = new TaskManager(broker.getId(), new ArrayList<>(1) {{
            add(new Workflow(tasks, null));
        }});

        var fogDevice0 = getDummyFogDevice(0);
        var fogDevice1 = getDummyFogDevice(1);
        var fogDevice2 = getDummyFogDevice(2);
        var fogDevice3 = getDummyFogDevice(3);

        CloudSim.startSimulation();
    }
}
