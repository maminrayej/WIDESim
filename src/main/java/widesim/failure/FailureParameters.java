package widesim.failure;

import org.cloudbus.cloudsim.Log;
import widesim.failure.DistributionGenerator.DistributionFamily;


public class FailureParameters {

    /**
     * Task Failure Rate
     * first index is vmId ;second index is task depth
     * If FAILURE_JOB is specified first index is 0 only
     * If FAILURE_VM is specified second index is 0 only
     *
     * @pre 0.0<= value <= 1.0
     */
    private static DistributionGenerator[][] generators;

    /**
     * FTC Monitor mode
     */
    public enum FTCMonitor {

        MONITOR_NONE, MONITOR_ALL, MONITOR_VM
    }

    /**
     * FTC Failure Generator mode
     */
    public enum FTCFailure {

        FAILURE_NONE, FAILURE_ALL, FAILURE_VM
    }

    /**
     * Fault Tolerant Clustering monitor mode
     */
    private static FTCMonitor monitorMode = FTCMonitor.MONITOR_NONE;
    /**
     * Fault Tolerant Clustering failure generation mode
     */
    private static FTCFailure failureMode = FTCFailure.FAILURE_NONE;

    /**
     * The distribution of the failure
     */
    private static DistributionFamily distribution = DistributionFamily.WEIBULL;
    /**
     * Invalid return value
     */
    private static final int INVALID = -1;

    /**
     *
     *  Init a FailureParameters
     *
     * @param monitor Fault Tolerant Clustering Monitor mode
     * @param failure Failure generator mode
     * @param failureGenerators
     */
    public static void init(FTCMonitor monitor, FTCFailure failure, DistributionGenerator[][] failureGenerators) {
        monitorMode = monitor;
        failureMode = failure;
        generators = failureGenerators;
    }

    /**
     *
     * Init a FailureParameters with distibution
     * @param monitor
     * @param dist
     * @param failureGenerators
     * @param failure
     */
    public static void init(FTCMonitor monitor, FTCFailure failure, DistributionGenerator[][] failureGenerators,
                            DistributionFamily dist) {
        distribution = dist;
        init(monitor, failure, failureGenerators);
    }
    /**
     * Gets the task failure rate
     *
     * @return the task failure rate
     * @pre $none
     * @post $none
     */
    public static DistributionGenerator[][] getFailureGenerators() {
        if(generators==null){
            Log.printLine("ERROR: alpha is not initialized");
        }
        return generators;
    }

    /**
     * Gets the max first index in alpha
     * @return max
     */
    public static int getFailureGeneratorsMaxFirstIndex(){
        if(generators==null || generators.length == 0){
            Log.printLine("ERROR: alpha is not initialized");
            return INVALID;
        }
        return generators.length;
    }

    /**
     * Gets the max second Index in alpha
     * @return max
     */
    public static int getFailureGeneratorsMaxSecondIndex(){
        //Test whether it is valid
        getFailureGeneratorsMaxFirstIndex();
        if(generators[0]==null || generators[0].length == 0){
            Log.printLine("ERROR: alpha is not initialized");
            return INVALID;
        }
        return generators[0].length;
    }


    /**
     * Gets the task failure rate
     * @param vmIndex vm Index
     * @param taskDepth task depth
     * @return task failure rate
     */
    public static DistributionGenerator getGenerator(int vmIndex, int taskDepth) {
        return generators[vmIndex][taskDepth];
    }

    /**
     * Gets the failure generation mode
     *
     * @return the failure generation mode
     * @pre $none
     * @post $none
     */
    public static FTCFailure getFailureGeneratorMode() {
        return failureMode;
    }

    /**
     * Gets the fault tolerant clustering monitor mode
     *
     * @return the fault tolerant clustering monitor mode
     * @pre $none
     * @post $none
     */
    public static FTCMonitor getMonitorMode() {
        return monitorMode;
    }

    /**
     * Gets the failure distribution
     * @return distribution
     */
    public static DistributionFamily getFailureDistribution(){
        return distribution;
    }
}

