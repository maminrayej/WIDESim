package entity;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;

import java.util.List;

public class FogHost extends PowerHost {

    private List<Vm> vms;

    public FogHost(int id,
                   RamProvisioner ramProvisioner,
                   BwProvisioner bwProvisioner,
                   long storage,
                   List<? extends Pe> peList,
                   VmScheduler vmScheduler,
                   PowerModel powerModel,
                   List<Vm> vms) {
        super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, powerModel);

        this.vms = vms;
    }

    public List<Vm> getVms() {
        return vms;
    }
}
