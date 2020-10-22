package misty.mapper;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Vm;

import java.util.List;
import java.util.Map;

public class SimpleVmToFogDeviceMapper implements VmToFogDeviceMapper {
    @Override
    public Map<Integer, Integer> map(Map<Integer, DatacenterCharacteristics> datacenterToCharacteristics, List<Vm> vms) {
        return null;
    }
}
