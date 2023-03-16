package widesim.examples.ifog;

import widesim.computation.Task;
import widesim.computation.Workflow;
import widesim.core.Logger;
import widesim.entity.FogBroker;
import widesim.entity.TaskManager;
import widesim.entity.WorkflowEngine;
import widesim.mapper.SimpleTaskToVmMapper;
import widesim.mapper.SimpleVmToFogDeviceMapper;
import widesim.parse.topology.Parser;
import widesim.parse.topology.PostProcessor;
import widesim.provision.SimpleVmProvisioner;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.stream.IntStream;

public class OneTaskOneDevice {
    public static void main(String[] args) throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);

        // Parse topology
        var topologyParser = new Parser(new File("src/main/resources/ifog/one_task_one_device/topology.json"));
        var deviceAndVms = topologyParser.parse();

        var fogDevices = deviceAndVms.getFirst();

        PostProcessor.connectHostToDatacenter(fogDevices);

        var topologyAnalyzer = PostProcessor.buildTopologyAnalyzer(fogDevices);
        var routingTable = topologyAnalyzer.buildRoutingTable();
        PostProcessor.setRoutingTableOfFogDevices(fogDevices, routingTable);
        var convertedRoutingTable = PostProcessor.convertNameToId(fogDevices, routingTable);

        var vms = deviceAndVms.getSecond();
        var fogBroker = new FogBroker("broker", new SimpleVmProvisioner(), new SimpleVmToFogDeviceMapper(), new SimpleTaskToVmMapper(), 1000L, 1000L, convertedRoutingTable, fogDevices);
        vms.forEach(vm -> {
            vm.setUid(Vm.getUid(fogBroker.getId(), vm.getId()));
            vm.setUserId(fogBroker.getId());
        });
        fogBroker.submitVmList(vms);

        // Parse workflows
        var workflowParser = new widesim.parse.workflow.Parser(new File("src/main/resources/ifog/one_task_one_device/workflows.json"));
        var workflows = workflowParser.parse();

        for (Workflow workflow: workflows) {
            widesim.parse.workflow.PostProcessor.connectChildTasksToParent(workflow);

            var analyzer = widesim.parse.workflow.PostProcessor.buildWorkflowAnalyzer(workflow);
            widesim.parse.workflow.PostProcessor.isWorkflowValid(analyzer);
        }

        var workflowEngine = new WorkflowEngine(fogBroker.getId());
        fogBroker.setWorkflowEngineId(workflowEngine.getId());

        var taskManager = new TaskManager(workflowEngine.getId(), workflows);
//        var taskManager = new TaskManager(fogBroker.getId(), workflows);

        CloudSim.startSimulation();

        CloudSim.stopSimulation();

        // Final step: Print results when simulation is over
//        List<Cloudlet> newList = fogBroker.getCloudletReceivedList();
//        printCloudletList(newList);
        List<Task> tasks = fogBroker.getReceivedTasks();

        IntStream.range(0, fogBroker.getMaximumCycle() + 1).forEach(cycle -> {
            System.out.println("Cycle: " + cycle);
            Logger.printResult(cycle, tasks, fogBroker.getVmToFogDevice(), fogDevices);
        });
    }
}
