package widesim.provision;

import widesim.computation.Task;
import org.cloudbus.cloudsim.power.PowerVm;
import org.jgrapht.alg.util.Triple;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface VmProvisioner {
    Triple<List<Integer>, List<Integer>, List<Integer>> provision(
            List<PowerVm> failedVms,
            List<PowerVm> createdVms,
            List<PowerVm> vms,
            Map<Integer, Integer> taskToVm,
            Set<Task> completedTasks,
            Set<Task> dispatchedTasks,
            List<Task> queuedTasks
    );
}
