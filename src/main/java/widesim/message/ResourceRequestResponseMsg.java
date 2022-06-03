package widesim.message;

import org.cloudbus.cloudsim.DatacenterCharacteristics;

public class ResourceRequestResponseMsg {

    private final DatacenterCharacteristics characteristics;

    public ResourceRequestResponseMsg(DatacenterCharacteristics characteristics) {
        this.characteristics = characteristics;
    }

    public DatacenterCharacteristics getCharacteristics() {
        return characteristics;
    }
}
