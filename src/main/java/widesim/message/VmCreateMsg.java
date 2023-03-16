package widesim.message;

import org.cloudbus.cloudsim.power.PowerVm;

public class VmCreateMsg {
    private final PowerVm vm;

    public VmCreateMsg(PowerVm vm) {
        this.vm = vm;
    }

    public PowerVm getVm() {
        return vm;
    }
}
