package widesim.examples.fail;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerVm;
import widesim.computation.Task;
import widesim.computation.Workflow;
import widesim.core.Logger;
import widesim.entity.FogBroker;
import widesim.entity.TaskManager;
import widesim.entity.WorkflowEngine;
import widesim.failure.DistributionGenerator;
import widesim.failure.FailureGenerator;
import widesim.failure.FailureMonitor;
import widesim.failure.FailureParameters;
import widesim.mapper.SimpleTaskToVmMapper;
import widesim.mapper.SimpleVmToFogDeviceMapper;
import widesim.parse.dax.DaxParser;
import widesim.parse.topology.Parser;
import widesim.parse.topology.PostProcessor;
import widesim.provision.SimpleVmProvisioner;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.stream.IntStream;

public class CyberShake100Fail {
    public static void main(String[] args) throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);

        // Parse topology
        var topologyParser = new Parser(new File("src/main/resources/topologies/new_topology.json"));
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
            vm.setUid(PowerVm.getUid(fogBroker.getId(), vm.getId()));
            vm.setUserId(fogBroker.getId());
        });
        fogBroker.submitVmList(vms);

        var daxParser = new DaxParser("src/main/resources/dax/CyberShake_100.xml");
        var workflows = List.of(daxParser.buildWorkflow());

        DistributionGenerator[][] failureGenerators = new DistributionGenerator[1][1];
//        failureGenerators[0][0] = new DistributionGenerator(DistributionGenerator.DistributionFamily.WEIBULL,
//                100, 1.0, 30, 300, 0.78);
        failureGenerators[0][0] = new DistributionGenerator(DistributionGenerator.DistributionFamily.NORMAL,
        0, 1.0, 30, 300, 0.78);

        FailureParameters.FTCMonitor ftc_monitor = FailureParameters.FTCMonitor.MONITOR_ALL;
        FailureParameters.FTCFailure ftc_failure = FailureParameters.FTCFailure.FAILURE_ALL;
        FailureParameters.init(ftc_monitor, ftc_failure, failureGenerators);
        FailureMonitor.init();
        FailureGenerator.init();


        for (Workflow workflow: workflows) {

            var analyzer = widesim.parse.workflow.PostProcessor.buildWorkflowAnalyzer(workflow);
            widesim.parse.workflow.PostProcessor.isWorkflowValid(analyzer);
        }

        var workflowEngine = new WorkflowEngine(fogBroker.getId());
        fogBroker.setWorkflowEngineId(workflowEngine.getId());

        var taskManager = new TaskManager(workflowEngine.getId(), workflows);

        CloudSim.startSimulation();

        CloudSim.stopSimulation();

        List<Task> tasks = fogBroker.getReceivedTasks();
        for (Vm vm : fogBroker.getVmList()) {
            PowerVm newvm = (PowerVm) vm;
            // System.out.println(vm.getTotalUtilizationOfCpu(0));
            // System.out.println(vm.get(0));
            System.out.println(newvm.getUtilizationMean());
        }
        IntStream.range(0, fogBroker.getMaximumCycle() + 1).forEach(cycle -> {
            System.out.println("Cycle: " + cycle);
            Logger.printResult(cycle, tasks, fogBroker.getVmToFogDevice(), fogDevices);
        });
    }
}
