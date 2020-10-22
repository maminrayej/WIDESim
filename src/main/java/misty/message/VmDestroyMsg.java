package misty.message;

public class VmDestroyMsg {
    private final int vmId;

    public VmDestroyMsg(int vmId) {
        this.vmId = vmId;
    }

    public int getVmId() {
        return vmId;
    }
}
