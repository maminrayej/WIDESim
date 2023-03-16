package widesim.mapper;

import widesim.computation.Task;
import org.cloudbus.cloudsim.power.PowerVm;
import org.jgrapht.alg.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleTaskToVmMapper implements TaskToVmMapper {
    @Override
    public Map<Integer, Integer> map(List<PowerVm> createdVms, List<PowerVm> failedVms, List<Task> queuedTasks,
                                     Set<Task> completedTasks, Set<Task> dispatchedTasks, Map<Integer, Integer> taskToVm,
                                     Map<Pair<Integer, Integer>, Integer> routingTable, Map<Integer, Integer> vmToFogDevice) {

        // Distribute tasks among created vms uniformly
        HashMap<Integer, Integer> newTaskToVm = new HashMap<>();

        if (!createdVms.isEmpty()) {
            for (Task task : queuedTasks) {
                int taskId = task.getTaskId();

                if (task.getAssignedVmId() != null) {
                    newTaskToVm.put(taskId, task.getAssignedVmId());
                } else {
                    int vmId = taskId % createdVms.size();

                    newTaskToVm.put(taskId, createdVms.get(vmId).getId());
                }
            }
        }

        return newTaskToVm;
    }
}
