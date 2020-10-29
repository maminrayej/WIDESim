package misty.computation;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class Task extends Cloudlet {

    private final List<Data> inputFiles;
    private final List<Integer> children;
    private final HashSet<Integer> parents;
    private final double deadLine;
    private final double entryTime;
    private final String workflowId;

    public Task(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
                UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw,
                List<Data> inputFiles, List<Integer> children, double deadLine, double entryTime, String workflowName) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.inputFiles = inputFiles;
        this.children = children;
        this.deadLine = deadLine;
        this.workflowId = workflowName;
        this.entryTime = entryTime;
        this.parents = new HashSet<>();
    }

    public List<Data> getInputFiles() {
        return inputFiles;
    }

    public List<Integer> getChildren() {
        return children;
    }

    public double getDeadLine() {
        return deadLine;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public int getTaskId() {
        return getCloudletId();
    }

    public double getEntryTime() {
        return entryTime;
    }

    public long getRuntime() {
        return super.getCloudletLength();
    }

    public long getTotalRuntime() {
        return super.getCloudletTotalLength();
    }

    public void addParentId(int parentId) {
        this.parents.add(parentId);
    }

    public List<Integer> getParents() {
        return new ArrayList<>(this.parents);
    }

    public long getTotalInputDataSize() {
        return this.inputFiles.stream().map(Data::getSize).reduce(0L, Long::sum);
    }
}
