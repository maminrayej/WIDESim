package widesim.core;

import widesim.entity.FogHost;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelCubic;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.List;

public class Enums {

    private Enums() {
        throw new UnsupportedOperationException("Can not instantiate class: Enums");
    }

    public static <T extends Enum<T>> T enumOf(Class<T> enumClass, String type) {
        T[] enumValues = enumClass.getEnumConstants();

        for (T mEnum : enumValues)
            if (mEnum.toString().equals(type))
                return mEnum;

        throw new IllegalArgumentException(String.format("%s is not a valid enum type of %s", type, enumClass.toString()));
    }


    public enum ArchEnum {
        X86_64("x86_64");

        private final String arch;

        ArchEnum(String arch) {
            this.arch = arch;
        }

        String getArch() {
            return arch;
        }

        @Override
        public String toString() {
            return getArch();
        }
    }

    public enum OsEnum {
        LINUX("Linux");

        private final String os;

        OsEnum(String os) {
            this.os = os;
        }

        String getOs() {
            return os;
        }

        @Override
        public String toString() {
            return getOs();
        }
    }

    public enum VmAllocPolicyEnum {
        SIMPLE("Simple");

        private final String policy;

        VmAllocPolicyEnum(String policy) {
            this.policy = policy;
        }

        public static VmAllocationPolicy getPolicy(String policy, List<FogHost> hosts) {
            VmAllocPolicyEnum policyEnum = enumOf(VmAllocPolicyEnum.class, policy);

            if (policyEnum == SIMPLE) {
                return new VmAllocationPolicySimple(hosts);
            } else {
                throw new IllegalArgumentException(String.format("%s is not a valid policy", policy));
            }
        }

        String getPolicy() {
            return this.policy;
        }

        @Override
        public String toString() {
            return this.getPolicy();
        }
    }

    public enum AllocPolicyEnum {
        SPACE_SHARED("Space Shared"),
        TIME_SHARED("Time Shared"),
        SAME_RATE("Same Rate"),
        DIFF_RATE("Diff Rate");

        private final String policy;

        AllocPolicyEnum(String policy) {
            this.policy = policy;
        }

        public static int getPolicy(String policy) {
            AllocPolicyEnum policyEnum = enumOf(AllocPolicyEnum.class, policy);

            switch (policyEnum) {
                case SPACE_SHARED:
                    return DatacenterCharacteristics.SPACE_SHARED;
                case SAME_RATE:
                    return DatacenterCharacteristics.OTHER_POLICY_SAME_RATING;
                case DIFF_RATE:
                    return DatacenterCharacteristics.OTHER_POLICY_DIFFERENT_RATING;
                case TIME_SHARED:
                default:
                    return DatacenterCharacteristics.TIME_SHARED;
            }
        }

        String getPolicy() {
            return this.policy;
        }

        @Override
        public String toString() {
            return this.getPolicy();
        }
    }

    public enum VmmEnum {
        XEN("Xen");

        private final String vmm;

        VmmEnum(String vmm) {
            this.vmm = vmm;
        }

        String getVmm() {
            return this.vmm;
        }

        @Override
        public String toString() {
            return getVmm();
        }
    }

    public enum VmSchedulerEnum {
        SPACE_SHARED("Space Shared"),
        TIME_SHARED("Time Shared"),
        TIME_SHARED_SUB("Time Shared Over Subscription");

        private final String scheduler;

        VmSchedulerEnum(String scheduler) {
            this.scheduler = scheduler;
        }

        public static VmScheduler getScheduler(String scheduler, List<Pe> pes) {
            VmSchedulerEnum schedulerEnum = enumOf(VmSchedulerEnum.class, scheduler);

            switch (schedulerEnum) {
                case SPACE_SHARED:
                    return new VmSchedulerSpaceShared(pes);
                case TIME_SHARED_SUB:
                    return new VmSchedulerTimeSharedOverSubscription(pes);
                case TIME_SHARED:
                default:
                    return new VmSchedulerTimeShared(pes);
            }
        }

        String getScheduler() {
            return this.scheduler;
        }

        @Override
        public String toString() {
            return getScheduler();
        }
    }

    public enum RamProvisionerEnum {
        SIMPLE("Simple");

        private final String provisioner;

        RamProvisionerEnum(String provisioner) {
            this.provisioner = provisioner;
        }

        public static RamProvisioner getProvisioner(String provisioner, int ram) {
            RamProvisionerEnum provisionerEnum = enumOf(RamProvisionerEnum.class, provisioner);

            if (provisionerEnum == SIMPLE) {
                return new RamProvisionerSimple(ram);
            } else {
                throw new IllegalArgumentException(String.format("%s is not a valid provisioner", provisioner));
            }
        }

        String getProvisioner() {
            return provisioner;
        }

        @Override
        public String toString() {
            return getProvisioner();
        }
    }

    public enum BwProvisionerEnum {
        SIMPLE("Simple");

        private final String provisioner;

        BwProvisionerEnum(String provisioner) {
            this.provisioner = provisioner;
        }

        public static BwProvisioner getProvisioner(String provisioner, long bw) {
            BwProvisionerEnum provisionerEnum = enumOf(BwProvisionerEnum.class, provisioner);

            if (provisionerEnum == SIMPLE) {
                return new BwProvisionerSimple(bw);
            } else {
                throw new IllegalArgumentException(String.format("%s is not a valid provisioner", provisioner));
            }
        }

        String getProvisioner() {
            return provisioner;
        }

        @Override
        public String toString() {
            return getProvisioner();
        }
    }

    public enum PeProvisionerEnum {
        SIMPLE("Simple");

        private final String provisioner;

        PeProvisionerEnum(String provisioner) {
            this.provisioner = provisioner;
        }

        public static PeProvisioner getProvisioner(String provisioner, double mips) {
            PeProvisionerEnum provisionerEnum = enumOf(PeProvisionerEnum.class, provisioner);

            if (provisionerEnum == SIMPLE) {
                return new PeProvisionerSimple(mips);
            } else {
                throw new IllegalArgumentException(String.format("%s is not a valid provisioner", provisioner));
            }
        }

        String getProvisioner() {
            return provisioner;
        }

        @Override
        public String toString() {
            return getProvisioner();
        }
    }

    public enum CloudletSchedulerEnum {
        TIME_SHARED("Time Shared"),
        SPACE_SHARED("Space Shared"),
        DYNAMIC("Dynamic");

        private final String scheduler;

        CloudletSchedulerEnum(String scheduler) {
            this.scheduler = scheduler;
        }

        public static CloudletScheduler getScheduler(String scheduler, double mips, int numOfPes) {
            CloudletSchedulerEnum schedulerEnum = enumOf(CloudletSchedulerEnum.class, scheduler);

            switch (schedulerEnum) {
                case SPACE_SHARED:
                    return new CloudletSchedulerSpaceShared();
                case DYNAMIC:
                    return new CloudletSchedulerDynamicWorkload(mips, numOfPes);
                case TIME_SHARED:
                default:
                    return new CloudletSchedulerTimeShared();
            }
        }

        String getScheduler() {
            return this.scheduler;
        }

        @Override
        public String toString() {
            return getScheduler();
        }
    }

    public enum PowerModelEnum {
        CUBIC("Cubic"),
        LINEAR("Linear");

        private final String powerModel;

        PowerModelEnum(String powerModel) {
            this.powerModel = powerModel;
        }

        public static PowerModel getPowerModel(String powerModel, double maxPower, double staticPowerPercent) {
            PowerModelEnum powerModelEnum = enumOf(PowerModelEnum.class, powerModel);

            switch (powerModelEnum) {
                case CUBIC:
                    return new PowerModelCubic(maxPower, staticPowerPercent);
                case LINEAR:
                default:
                    return new PowerModelLinear(maxPower, staticPowerPercent);
            }
        }

        String getPowerModel() {
            return powerModel;
        }

        @Override
        public String toString() {
            return getPowerModel();
        }
    }

    public enum UtilizationModelEnum {
        Full("Full"),
        STOCHASTIC("Stochastic");

        private final String utilizationModel;

        UtilizationModelEnum(String utilizationModel) {
            this.utilizationModel = utilizationModel;
        }

        public String getUtilizationModel() {
            return utilizationModel;
        }

        @Override
        public String toString() {
            return getUtilizationModel();
        }

        public static UtilizationModel getUtilizationModel(String utilizationModel) {
            UtilizationModelEnum utilizationModelEnum = enumOf(UtilizationModelEnum.class, utilizationModel);

            switch (utilizationModelEnum) {
                case STOCHASTIC:
                    return new UtilizationModelStochastic();
                case Full:
                default:
                    return new UtilizationModelFull();
            }
        }
    }

}
