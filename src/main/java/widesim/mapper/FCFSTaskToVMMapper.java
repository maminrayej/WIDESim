package widesim.mapper;

import org.cloudbus.cloudsim.power.PowerVm;
import org.jgrapht.alg.util.Pair;
import widesim.computation.Task;

import java.util.*;

public class FCFSTaskToVMMapper implements TaskToVmMapper {
    HashSet<Integer> usedVMs;

    public FCFSTaskToVMMapper() {
        this.usedVMs = new HashSet<>();
    }

    public void releaseVM(int vmID) {
        this.usedVMs.remove(vmID);
    }

    @Override
    public Map<Integer, Integer> map(List<PowerVm> createdVms, List<PowerVm> failedVms, List<Task> queuedTasks, Set<Task> completedTasks, Set<Task> dispatchedTasks, Map<Integer, Integer> taskToVm, Map<Pair<Integer, Integer>, Integer> routingTable, Map<Integer, Integer> vmToFogDevice) {

        HashMap<Integer, Integer> newTaskToVm = new HashMap<>();

        for (Task queuedTask : queuedTasks) {
            if (queuedTask.getAssignedVmId() != null) {
                newTaskToVm.put(queuedTask.getTaskId(), queuedTask.getAssignedVmId());
            } else {
                boolean stillHasVm = false;

                for (PowerVm createdVm : createdVms) {

                    if (!usedVMs.contains(createdVm.getId())) {
                        stillHasVm = true;
                        usedVMs.add(createdVm.getId());
                        queuedTask.setVmId(createdVm.getId());
                        newTaskToVm.put(queuedTask.getTaskId(), createdVm.getId());
                        break;
                    }
                }
                if (!stillHasVm) {
                    break;
                }
            }
        }
        return newTaskToVm;
    }
}
