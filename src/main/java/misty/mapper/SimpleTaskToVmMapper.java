package misty.mapper;

import misty.computation.Task;
import org.cloudbus.cloudsim.Vm;

import java.util.List;
import java.util.Map;

public class SimpleTaskToVmMapper implements TaskToVmMapper {
    @Override
    public Map<Integer, Integer> map(List<Vm> createdVms, List<Vm> failedVms, List<Task> tasks) {
        return null;
    }
}
