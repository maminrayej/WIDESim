package misty.computation;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Task extends Cloudlet {

    private final List<Data> inputFiles;
    private final List<Integer> children;
    private HashSet<Integer> parents;
    private final double deadLine;
    private final double entryTime;
    private final String workflowId;

    private SelectivityModel selectivityModel;
    private HashMap<Integer, Boolean> cycleToGeneratedData;

    private ExecutionModel executionModel;

    private int cycle = 0;

    private TaskState taskState;

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

        this.selectivityModel = new FractionalSelectivity(0.5f);
        this.cycleToGeneratedData = new HashMap<>();

        this.executionModel = new PeriodicExecutionModel(1f);

        this.taskState = new TaskState();
    }

    public Task(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
                UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw,
                List<Data> inputFiles, List<Integer> children, double deadLine, double entryTime, String workflowName, SelectivityModel selectivityModel, ExecutionModel executionModel) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.inputFiles = inputFiles;
        this.children = children;
        this.deadLine = deadLine;
        this.workflowId = workflowName;
        this.entryTime = entryTime;
        this.parents = new HashSet<>();

        this.cycleToGeneratedData = new HashMap<>();

        this.taskState = new TaskState();

        this.selectivityModel = selectivityModel;

        this.executionModel = executionModel;
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

    public int getCycle() {
        return cycle;
    }

    public void setCycle(int cycle) {
        this.cycle = cycle;
    }

    public void goToNextCycle() {
        this.cycle++;
    }

    public void setParents(HashSet<Integer> parents) {
        this.parents = parents;
    }

    public void setSelectivityModel(SelectivityModel selectivityModel) {
        this.selectivityModel = selectivityModel;
    }

    public void setCycleToGeneratedData(HashMap<Integer, Boolean> cycleToGeneratedData) {
        this.cycleToGeneratedData = cycleToGeneratedData;
    }

    public HashMap<Integer, Boolean> getCycleToGeneratedData() {
        return cycleToGeneratedData;
    }

    public void setExecutionModel(ExecutionModel executionModel) {
        this.executionModel = executionModel;
    }

    public void setTaskState(TaskState taskState) {
        this.taskState = taskState;
    }

    public TaskState getTaskState() {
        return taskState;
    }

    public Task getNextCycle() {
        var task = new Task(
                getCloudletId(), getCloudletLength(), getNumberOfPes(), getCloudletFileSize(),
                getCloudletOutputSize(), getUtilizationModelCpu(), getUtilizationModelRam(),
                getUtilizationModelBw(), getInputFiles(), getChildren(), getDeadLine(),
                getEntryTime(), getWorkflowId()
        );

        task.setUserId(this.getUserId());
        task.setVmId(this.getVmId());
        task.setParents(this.parents);
        task.setSelectivityModel(this.selectivityModel);
        task.setCycleToGeneratedData(this.cycleToGeneratedData);
        task.setCycle(this.cycle);
        task.setExecutionModel(this.executionModel);
        task.setTaskState(this.taskState);

        task.goToNextCycle();

        return task;
    }

    public boolean wantToGenerateData(int cycle, double clock) {
        cycleToGeneratedData.computeIfAbsent(cycle, k -> selectivityModel.generateData(clock));

        return cycleToGeneratedData.get(cycle);
    }

    public boolean didYouGenerateData(int cycle) {
        return cycleToGeneratedData.getOrDefault(cycle, false);
    }

    public double getNextTimeExecution(double clock) {
        return executionModel.nextExecutionTime(clock);
    }
}
