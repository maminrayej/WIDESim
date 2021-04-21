package misty.mapper;

import misty.computation.Task;
import org.cloudbus.cloudsim.Vm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleTaskToVmMapper implements TaskToVmMapper {
    @Override
    public Map<Integer, Integer> map(List<Vm> createdVms,
                                     List<Vm> failedVms,
                                     List<Task> queuedTasks,
                                     Set<Task> completedTasks,
                                     Set<Task> dispatchedTasks,
                                     Map<Integer, Integer> taskToVm) {

        // Distribute tasks among created vms uniformly
        HashMap<Integer, Integer> newTaskToVm = new HashMap<>();

        if (!createdVms.isEmpty()) {
            for (Task task : queuedTasks) {
                int taskId = task.getTaskId();

                if (task.getAssignedVmId() != null) {
                    newTaskToVm.put(taskId, task.getAssignedVmId());
                } else {
                    int vmId = taskId % createdVms.size();

                    newTaskToVm.put(taskId, vmId);
                }
            }
        }

        return newTaskToVm;
    }
}
