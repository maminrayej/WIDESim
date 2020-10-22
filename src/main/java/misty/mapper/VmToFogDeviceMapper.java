package misty.mapper;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Vm;

import java.util.List;
import java.util.Map;

public interface VmToFogDeviceMapper {

    // vmId -> datacenterId
    Map<Integer, Integer> map(Map<Integer, DatacenterCharacteristics> datacenterToCharacteristics, List<Vm> vms);
}
