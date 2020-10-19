package misty.mapper;

import misty.computation.Task;
import org.cloudbus.cloudsim.Vm;

import java.util.List;
import java.util.Map;

public interface TaskToVmMapper {
    Map<Integer, Integer> map(List<Vm> createdVms, List<Vm> failedVms, List<Task> tasks);
}
