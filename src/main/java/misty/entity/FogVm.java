package misty.entity;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;

public class FogVm extends Vm {
    public FogVm(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm, CloudletScheduler cloudletScheduler) {
        super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
    }

    public void setUserId(int userId) {
        super.setUserId(userId);
    }
}
