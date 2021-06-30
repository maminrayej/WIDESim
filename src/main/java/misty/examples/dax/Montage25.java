package misty.examples.dax;

import misty.computation.Task;
import misty.computation.Workflow;
import misty.core.Logger;
import misty.entity.FogBroker;
import misty.entity.TaskManager;
import misty.entity.WorkflowEngine;
import misty.mapper.SimpleTaskToVmMapper;
import misty.mapper.SimpleVmToFogDeviceMapper;
import misty.parse.dax.DaxParser;
import misty.parse.topology.Parser;
import misty.parse.topology.PostProcessor;
import misty.provision.SimpleVmProvisioner;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Montage25 {
    public static void main(String[] args) throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);

        // Parse topology
        var topologyParser = new Parser(new File("src/main/resources/one_task_one_device/topology.json"));
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

        var daxParser = new DaxParser("src/main/resources/dax/HEFT_paper.xml");
        var workflows = List.of(daxParser.buildWorkflow());

        for (Workflow workflow: workflows) {

            var analyzer = misty.parse.workflow.PostProcessor.buildWorkflowAnalyzer(workflow);
            misty.parse.workflow.PostProcessor.isWorkflowValid(analyzer);
        }

        var workflowEngine = new WorkflowEngine(fogBroker.getId());
        fogBroker.setWorkflowEngineId(workflowEngine.getId());

        var taskManager = new TaskManager(workflowEngine.getId(), workflows);

        CloudSim.startSimulation();

        CloudSim.stopSimulation();

        List<Task> tasks = fogBroker.getReceivedTasks();

        IntStream.range(0, fogBroker.getMaximumCycle() + 1).forEach(cycle -> {
            System.out.println("Cycle: " + cycle);
            Logger.printResult(cycle, tasks, fogBroker.getVmToFogDevice(), fogDevices);
        });
    }
}
