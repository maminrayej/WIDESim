package misty.computation;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

import java.util.List;

public class Task extends Cloudlet {

    private final List<Data> inputFiles;
    private final List<String> children;
    private final double deadLine;
    private final String workflowId;

    public Task(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
                UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw,
                List<Data> inputFiles, List<String> children, double deadLine, String workflowName) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.inputFiles = inputFiles;
        this.children = children;
        this.deadLine = deadLine;
        this.workflowId = workflowName;
    }

    public List<Data> getInputFiles() {
        return inputFiles;
    }

    public List<String> getChildren() {
        return children;
    }

    public double getDeadLine() {
        return deadLine;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getTaskId() {
        return String.valueOf(super.getCloudletId());
    }

}
