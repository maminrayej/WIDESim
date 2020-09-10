package parse;

import core.Constants;
import core.Enums.*;

public class Default {

    private Default() {
        throw new UnsupportedOperationException("Can not instantiate class: Default");
    }

    public static class FOG_DEVICE {
        public static final ArchEnum ARCH = ArchEnum.X86_64;
        public static final OsEnum OS = OsEnum.LINUX;
        public static final double TIME_ZONE = 0.0;
        public static final double COST_PER_SEC = 1.0;
        public static final double COST_PER_MEM = 1.0;
        public static final double COST_PER_BW = 1.0;
        public static final double COST_PER_STORAGE = 1.0;
        public static final VmAllocPolicyEnum VM_ALLOC_POLICY = VmAllocPolicyEnum.SIMPLE;
        public static final AllocPolicyEnum ALLOC_POLICY = AllocPolicyEnum.TIME_SHARED;
        public static final VmmEnum VMM = VmmEnum.XEN;
        public static final double SCHEDULING_INTERVAL = 0.0;
    }

    public static class HOST {
        public static final long STORAGE_CAP = Constants.DataUnit.GB;
        public static final int RAM = Constants.PowOfTwo.NINE;
        public static final long BW = Constants.PowOfTwo.TEN;
        public static final VmSchedulerEnum VM_SCHEDULER = VmSchedulerEnum.TIME_SHARED;
        public static final RamProvisionerEnum RAM_PROVISIONER = RamProvisionerEnum.SIMPLE;
        public static final BwProvisionerEnum BW_PROVISIONER = BwProvisionerEnum.SIMPLE;
    }

    public static class VM {
        public static final long SIZE = Constants.DataUnit.MB;
        public static final double MIPS = Constants.MetricUnit.GIGA;
        public static final int NUM_OF_PES = 1;
        public static final int RAM = Constants.PowOfTwo.NINE;
        public static final long BW = Constants.PowOfTwo.TEN;
        public static final VmmEnum VMM = VmmEnum.XEN;
        public static final CloudletSchedulerEnum CLOUDLET_SCHEDULER = CloudletSchedulerEnum.TIME_SHARED;
    }

    public static class PE {
        public static final double MIPS = Constants.MetricUnit.GIGA;
        public static final PeProvisionerEnum PE_PROVISIONING = PeProvisionerEnum.SIMPLE;
    }

}
