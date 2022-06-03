package widesim.entity;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;

import java.util.List;

public class FogHost extends Host {

    public FogHost(int id,
                   RamProvisioner ramProvisioner,
                   BwProvisioner bwProvisioner,
                   long storage,
                   List<? extends Pe> peList,
                   VmScheduler vmScheduler,
                   PowerModel powerModel) {
        super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
//        super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, powerModel);

    }

}
