package misty.examples;

import misty.computation.Workflow;
import misty.entity.FogBroker;
import misty.entity.TaskManager;
import misty.mapper.SimpleTaskToVmMapper;
import misty.mapper.SimpleVmToFogDeviceMapper;
import misty.parse.topology.Parser;
import misty.parse.topology.PostProcessor;
import misty.provision.SimpleVmProvisioner;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;

public class ThreeTaskTwoDevice {
    public static void main(String[] args) throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);

        // Parse topology
        var topologyParser = new Parser(new File("/home/amin/projects/java/Misty/src/main/resources/three_task_two_device/topology.json"));
        var deviceAndVms = topologyParser.parse();

        var fogDevices = deviceAndVms.getFirst();

        PostProcessor.connectHostToDatacenter(fogDevices);

        var topologyAnalyzer = PostProcessor.buildTopologyAnalyzer(fogDevices);
        var routingTable = topologyAnalyzer.buildRoutingTable();
//        System.out.println(">>> Routing table: " + routingTable);
        PostProcessor.setRoutingTableOfFogDevices(fogDevices, routingTable);

        var vms = deviceAndVms.getSecond();
        var fogBroker = new FogBroker("broker", new SimpleVmProvisioner(), new SimpleVmToFogDeviceMapper(), new SimpleTaskToVmMapper(), 10L, 10L);
        vms.forEach(vm -> {
            vm.setUid(Vm.getUid(fogBroker.getId(), vm.getId()));
            vm.setUserId(fogBroker.getId());
        });
        fogBroker.submitVmList(vms);

        // Parse workflows
        var workflowParser = new misty.parse.workflow.Parser(new File("/home/amin/projects/java/Misty/src/main/resources/three_task_two_device/workflows.json"));
        var workflows = workflowParser.parse();

        for (Workflow workflow: workflows) {
            misty.parse.workflow.PostProcessor.connectChildTasksToParent(workflow);

            var analyzer = misty.parse.workflow.PostProcessor.buildWorkflowAnalyzer(workflow);
            misty.parse.workflow.PostProcessor.isWorkflowValid(analyzer);
        }

        var taskManager = new TaskManager(fogBroker.getId(), workflows);

        CloudSim.startSimulation();

        CloudSim.stopSimulation();

        // Final step: Print results when simulation is over
        List<Cloudlet> newList = fogBroker.getCloudletReceivedList();
        printCloudletList(newList);
    }

    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent + "VM ID" + indent + "Time" + indent
                + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");

                Log.printLine(indent + indent + cloudlet.getResourceId()
                        + indent + indent + indent + cloudlet.getVmId()
                        + indent + indent
                        + dft.format(cloudlet.getActualCPUTime()) + indent
                        + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent
                        + dft.format(cloudlet.getFinishTime()));
            }
        }
    }
}
