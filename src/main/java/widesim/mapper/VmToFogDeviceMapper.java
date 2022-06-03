package widesim.mapper;

import widesim.entity.FogDevice;
import widesim.entity.FogVm;
import org.cloudbus.cloudsim.DatacenterCharacteristics;

import java.util.List;
import java.util.Map;

public interface VmToFogDeviceMapper {

    // vmId -> datacenterId
    Map<Integer, Integer> map(Map<Integer, DatacenterCharacteristics> datacenterToCharacteristics, List<FogVm> vms, List<FogDevice> fogDevices);
}
