package misty.entity;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;

public class FogVm extends Vm {
    private Integer assignedFogDeviceId;

    public FogVm(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm, CloudletScheduler cloudletScheduler) {
        super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);

        this.assignedFogDeviceId = null;
    }

    public FogVm(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm, CloudletScheduler cloudletScheduler, Integer assignedFogDeviceId) {
        super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);

        this.assignedFogDeviceId = assignedFogDeviceId;
    }

    public Integer getAssignedFogDeviceId() {
        return assignedFogDeviceId;
    }

    public void setAssignedFogDeviceId(Integer assignedFogDeviceId) {
        this.assignedFogDeviceId = assignedFogDeviceId;
    }

    public void setUserId(int userId) {
        super.setUserId(userId);
    }
}
