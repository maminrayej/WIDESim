package misty.mapper;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class SimpleVmToFogDeviceMapper implements VmToFogDeviceMapper {
    @Override
    public Map<Integer, Integer> map(Map<Integer, DatacenterCharacteristics> datacenterToCharacteristics, List<Vm> vms) {
        // Distribute vms among fog devices uniformly
        HashMap<Integer, Integer> vmToFogDevice = new HashMap<>();

        var fogDeviceIds = new ArrayList<>(datacenterToCharacteristics.keySet());

        IntStream.range(0, vms.size()).forEach(vmIndex -> {
            int fogDeviceIndex = vmIndex % fogDeviceIds.size();

            vmToFogDevice.put(vms.get(vmIndex).getId(), fogDeviceIds.get(fogDeviceIndex));
        });

        return vmToFogDevice;
    }
}
