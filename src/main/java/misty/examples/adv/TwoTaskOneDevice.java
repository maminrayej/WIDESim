package misty.examples.adv;

import misty.computation.Task;
import misty.computation.Workflow;
import misty.core.Logger;
import misty.entity.FogBroker;
import misty.entity.TaskManager;
import misty.entity.WorkflowEngine;
import misty.mapper.SimpleTaskToVmMapper;
import misty.mapper.SimpleVmToFogDeviceMapper;
import misty.parse.topology.Parser;
import misty.parse.topology.PostProcessor;
import misty.provision.SimpleVmProvisioner;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.stream.IntStream;

public class TwoTaskOneDevice {
    public static void main(String[] args) throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);

        // Parse topology
        var topologyParser = new Parser(new File("src/main/resources/adv/two_task_one_device/topology.json"));
        var deviceAndVms = topologyParser.parse();

        var fogDevices = deviceAndVms.getFirst();

        PostProcessor.connectHostToDatacenter(fogDevices);

        var topologyAnalyzer = PostProcessor.buildTopologyAnalyzer(fogDevices);
        var routingTable = topologyAnalyzer.buildRoutingTable();
        PostProcessor.setRoutingTableOfFogDevices(fogDevices, routingTable);
        var convertedRoutingTable = PostProcessor.convertNameToId(fogDevices, routingTable);

        var vms = deviceAndVms.getSecond();
        var fogBroker = new FogBroker("broker", new SimpleVmProvisioner(), new SimpleVmToFogDeviceMapper(), new SimpleTaskToVmMapper(), 10L, 10L, convertedRoutingTable, fogDevices);
        vms.forEach(vm -> {
            vm.setUid(Vm.getUid(fogBroker.getId(), vm.getId()));
            vm.setUserId(fogBroker.getId());
        });
        fogBroker.submitVmList(vms);

        // Parse workflows
        var workflowParser = new misty.parse.workflow.Parser(new File("src/main/resources/adv/two_task_one_device/workflows.json"));
        var workflows = workflowParser.parse();

        for (Workflow workflow : workflows) {
            misty.parse.workflow.PostProcessor.connectChildTasksToParent(workflow);

            var analyzer = misty.parse.workflow.PostProcessor.buildWorkflowAnalyzer(workflow);
            misty.parse.workflow.PostProcessor.isWorkflowValid(analyzer);
        }

        var workflowEngine = new WorkflowEngine(fogBroker.getId());
        fogBroker.setWorkflowEngineId(workflowEngine.getId());

        var taskManager = new TaskManager(workflowEngine.getId(), workflows);

//        var taskManager = new TaskManager(fogBroker.getId(), workflows);

        CloudSim.startSimulation();

        CloudSim.stopSimulation();

        //Final step: Print results when simulation is over
        List<Task> tasks = fogBroker.getReceivedTasks();

        IntStream.range(0, fogBroker.getMaximumCycle() + 1).forEach(cycle -> {
            System.out.println("Cycle: " + cycle);
            Logger.printResult(cycle, tasks, fogBroker.getVmToFogDevice(), fogDevices);
        });
    }
}
