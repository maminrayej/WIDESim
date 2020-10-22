package misty.message;

public class VmCreateAckMsg {

    public final int datacenterId;
    public final int vmId;
    public final boolean isCreated;

    public VmCreateAckMsg(int datacenterId, int vmId, boolean isCreated) {
        this.datacenterId = datacenterId;
        this.vmId = vmId;
        this.isCreated = isCreated;
    }

    public int getDatacenterId() {
        return datacenterId;
    }

    public int getVmId() {
        return vmId;
    }

    public boolean isCreated() {
        return isCreated;
    }
}
