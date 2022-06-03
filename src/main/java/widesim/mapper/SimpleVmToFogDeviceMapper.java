package widesim.mapper;

import widesim.entity.FogDevice;
import widesim.entity.FogVm;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class SimpleVmToFogDeviceMapper implements VmToFogDeviceMapper {
    @Override
    public Map<Integer, Integer> map(Map<Integer, DatacenterCharacteristics> datacenterToCharacteristics, List<FogVm> vms, List<FogDevice> fogDevices) {
        HashMap<Integer, Integer> vmToFogDevice = new HashMap<>();

        var fogDeviceIds = new ArrayList<>(datacenterToCharacteristics.keySet());

        IntStream.range(0, vms.size()).forEach(vmIndex -> {
            FogVm vm = vms.get(vmIndex);
            if (vm.getAssignedFogDeviceId() != null) {
                FogDevice fogDevice = fogDevices.stream().filter(fd -> fd.getName().equals(vm.getAssignedFogDeviceId())).findAny().orElse(null);
                vmToFogDevice.put(vm.getId(), fogDevice.getId());
            } else {
                int fogDeviceIndex = vmIndex % fogDeviceIds.size();

                vmToFogDevice.put(vm.getId(), fogDeviceIds.get(fogDeviceIndex));
            }
        });

        return vmToFogDevice;
    }
}
