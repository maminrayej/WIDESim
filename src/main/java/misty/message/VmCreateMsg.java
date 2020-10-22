package misty.message;

import org.cloudbus.cloudsim.Vm;

public class VmCreateMsg {
    private final Vm vm;

    public VmCreateMsg(Vm vm) {
        this.vm = vm;
    }

    public Vm getVm() {
        return vm;
    }
}
