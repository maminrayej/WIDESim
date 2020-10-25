package misty.provision;

import misty.computation.Task;
import org.cloudbus.cloudsim.Vm;
import org.jgrapht.alg.util.Triple;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleVmProvisioner implements VmProvisioner {

    @Override
    public Triple<List<Integer>, List<Integer>, List<Integer>> provision(List<Vm> failedVms,
                                                                         List<Vm> createdVms,
                                                                         List<Vm> vms,
                                                                         Map<Integer, Integer> taskToVm,
                                                                         Set<Task> completedTasks,
                                                                         Set<Task> dispatchedTasks,
                                                                         List<Task> queuedTasks) {
        return null;
    }
}
