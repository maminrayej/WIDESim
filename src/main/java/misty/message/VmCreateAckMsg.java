package misty.message;

public class VmCreateAckMsg {

    public final int fogDeviceId;
    public final int vmId;
    public final boolean isCreated;

    public VmCreateAckMsg(int fogDeviceId, int vmId, boolean isCreated) {
        this.fogDeviceId = fogDeviceId;
        this.vmId = vmId;
        this.isCreated = isCreated;
    }

    public int getFogDeviceId() {
        return fogDeviceId;
    }

    public int getVmId() {
        return vmId;
    }

    public boolean isCreated() {
        return isCreated;
    }
}
