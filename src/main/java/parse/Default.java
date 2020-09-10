package parse;

import core.Constants;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.List;

public class Default {

    private Default() {
        throw new UnsupportedOperationException("Can not instantiate class: Default");
    }

    public static class VM {
        public static final long SIZE = Constants.DataUnit.MB;
        public static final double MIPS = Constants.MetricUnit.GIGA;
        public static final int NUM_OF_PES = 1;
        public static final int RAM = Constants.PowOfTwo.NINE;
        public static final long BW = Constants.PowOfTwo.TEN;
        public static final String VMM = "Xen";
        public static final String CLOUDLET_SCHEDULER = "Time Shared";

        public static CloudletScheduler cloudletScheduler() {
            return new CloudletSchedulerSpaceShared();
        }
    }

    public static class HOST {
        public static final long STORAGE_CAP = Constants.DataUnit.GB;
        public static final int RAM = Constants.PowOfTwo.NINE;
        public static final long BW = Constants.PowOfTwo.TEN;
        public static final String VM_SCHEDULER = "Time Shared";
        public static final String RAM_PROVISIONER = "Simple";
        public static final String BW_PROVISIONER = "Simple";

        public static RamProvisioner ramProvisioner(int ram) {
            return new RamProvisionerSimple(ram);
        }

        public static BwProvisioner bwProvisioner(long bw) {
            return new BwProvisionerSimple(bw);
        }

        public static VmScheduler vmScheduler(List<Pe> pes) {
            return new VmSchedulerTimeShared(pes);
        }
    }

    public static class PE {
        public static final double MIPS = Constants.MetricUnit.GIGA;
        public static final String PE_PROVISIONING = "Simple";

        public static PeProvisioner peProvisioner(double mips) {
            return new PeProvisionerSimple(mips);
        }
    }

    public static class FOG_DEVICE {
        public static final String ARCH = "x86_64";
        public static final String OS = "Linux";
        public static final double TIME_ZONE = 0.0;
        public static final double COST_PER_SEC = 1.0;
        public static final double COST_PER_MEM = 1.0;
        public static final double COST_PER_BW = 1.0;
        public static final double COST_PER_STORAGE = 1.0;
        public static final String VM_ALLOC_POLICY = "Simple";
        public static final String ALLOC_POLICY = "Time Shared";
        public static final String VMM = "Xen";
        public static final double SCHEDULING_INTERVAL = 0.0;

        public static VmAllocationPolicy vmAllocationPolicy(List<Host> hosts) {
            return new VmAllocationPolicySimple(hosts);
        }
    }
}
