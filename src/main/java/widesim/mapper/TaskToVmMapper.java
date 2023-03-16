package widesim.mapper;

import widesim.computation.Task;
import org.cloudbus.cloudsim.power.PowerVm;
import org.jgrapht.alg.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TaskToVmMapper {
    Map<Integer, Integer> map(List<PowerVm> createdVms,
                              List<PowerVm> failedVms,
                              List<Task> queuedTasks,
                              Set<Task> completedTasks,
                              Set<Task> dispatchedTasks,
                              Map<Integer, Integer> taskToVm,
                              Map<Pair<Integer, Integer>, Integer> routingTable,
                              Map<Integer, Integer> vmToFogDevice);
}
