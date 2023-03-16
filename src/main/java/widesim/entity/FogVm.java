package widesim.entity;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.power.PowerVm;

public class FogVm extends PowerVm {
    private String assignedFogDeviceId;

    public FogVm(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm, CloudletScheduler cloudletScheduler) {
        super(id, userId, mips, numberOfPes, ram, bw, size, 1, vmm, cloudletScheduler, 300);

        this.assignedFogDeviceId = null;
    }

    public FogVm(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm, CloudletScheduler cloudletScheduler, String assignedFogDeviceId) {
        super(id, userId, mips, numberOfPes, ram, bw, size, 1, vmm, cloudletScheduler, 300);

        this.assignedFogDeviceId = assignedFogDeviceId;
    }

    public String getAssignedFogDeviceId() {
        return assignedFogDeviceId;
    }

    public void setAssignedFogDeviceId(String assignedFogDeviceId) {
        this.assignedFogDeviceId = assignedFogDeviceId;
    }

    public void setUserId(int userId) {
        super.setUserId(userId);
    }

    public void setCloudletScheduler(CloudletScheduler cloudletScheduler) {
        super.setCloudletScheduler(cloudletScheduler);
    }
}
