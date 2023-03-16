package widesim.provision;

import widesim.computation.Task;
import org.cloudbus.cloudsim.power.PowerVm;
import org.jgrapht.alg.util.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SimpleVmProvisioner implements VmProvisioner {

    @Override
    public Triple<List<Integer>, List<Integer>, List<Integer>> provision(List<PowerVm> failedVms,
                                                                         List<PowerVm> createdVms,
                                                                         List<PowerVm> vms,
                                                                         Map<Integer, Integer> taskToVm,
                                                                         Set<Task> completedTasks,
                                                                         Set<Task> dispatchedTasks,
                                                                         List<Task> queuedTasks) {

        // do not change vms:
        // to be created: empty
        // to be destroyed: empty
        // to stay alive: already created vms
        return Triple.of(new ArrayList<>(), new ArrayList<>(), createdVms.stream().map(PowerVm::getId).collect(Collectors.toList()));
    }
}
