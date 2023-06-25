package widesim.computation;

import org.apache.commons.lang3.ArrayUtils;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Consts;
import org.cloudbus.cloudsim.UtilizationModel;

import java.util.*;

public class Task extends Cloudlet {

    private Double ram;
    private Double bw;
    private final Integer assignedVmId;
    private final List<Data> inputFiles;
    private List<Integer> children;
    private Set<Integer> parents;
    private final double deadLine;
    private final double entryTime;
    private final String workflowId;

    private SelectivityModel selectivityModel;
    private HashMap<Integer, Boolean> cycleToGeneratedData;

    private ExecutionModel executionModel;

    private int cycle = 0;

    private TaskState taskState;

    private Map<String, Long> fileMap;
    private Map<Integer, List<String>> neededFromParent;
    private ArrayList<Double> failedExecutions;

    public void setFileMap(Map<String, Long> fileMap) {
        this.fileMap = fileMap;
    }

    public void setNeededFromParent(Map<Integer, List<String>> neededFromParent) {
        this.neededFromParent = neededFromParent;
    }

    public Task(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
                UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw,
                List<Data> inputFiles, double deadLine, double entryTime, String workflowName, ArrayList<Double> failedExecutions) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.inputFiles = inputFiles;
        this.deadLine = deadLine;
        this.workflowId = workflowName;
        this.entryTime = entryTime;
        this.parents = new HashSet<>();

        this.selectivityModel = new FractionalSelectivity(0.5f);
        this.cycleToGeneratedData = new HashMap<>();

        this.executionModel = new PeriodicExecutionModel(1f);

        this.taskState = new TaskState();

        this.ram = null;
        this.bw = null;
        this.assignedVmId = null;
        this.failedExecutions = failedExecutions;
    }

    public Task(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
                UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw,
                List<Data> inputFiles, double deadLine, double entryTime, String workflowName, SelectivityModel selectivityModel, ExecutionModel executionModel,
                Double ram, Double bw, Integer assignedVmId) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.inputFiles = inputFiles;
        this.deadLine = deadLine;
        this.workflowId = workflowName;
        this.entryTime = entryTime;
        this.parents = new HashSet<>();

        this.cycleToGeneratedData = new HashMap<>();

        this.taskState = new TaskState();

        this.selectivityModel = selectivityModel;

        this.executionModel = executionModel;

        this.ram = ram;
        this.bw = bw;
        this.assignedVmId = assignedVmId;
        this.failedExecutions = new ArrayList<>();
    }

    public Double getRam() {
        return ram;
    }

    public Double getBw() {
        return bw;
    }

    public Integer getAssignedVmId() { return this.assignedVmId; }

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

    public double getTotalInputDataSize() {
        return this.inputFiles.stream().map(Data::getSize).reduce(0.0, Double::sum);
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

    public void setParents(Set<Integer> parents) {
        this.parents = parents;
    }

    public void setChildren(List<Integer> children) {
        this.children = children;
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
                getUtilizationModelBw(), getInputFiles(), getDeadLine(),
                getEntryTime(), getWorkflowId(), getFailedExecutions()
        );

        task.setUserId(this.getUserId());
        task.setVmId(this.getVmId());
        task.setParents(this.parents);
        task.setChildren(this.children);
        task.setSelectivityModel(this.selectivityModel);
        task.setCycleToGeneratedData(this.cycleToGeneratedData);
        task.setCycle(this.cycle);
        task.setExecutionModel(this.executionModel);
        task.setTaskState(this.taskState);
        task.setFileMap(this.fileMap);
        task.setNeededFromParent(this.neededFromParent);

        task.goToNextCycle();

        return task;
    }

    public boolean wantToGenerateData(int cycle, double clock) {
        cycleToGeneratedData.computeIfAbsent(cycle, k -> selectivityModel.generateData(clock));

        return cycleToGeneratedData.get(cycle);
    }

    public Boolean didYouGenerateData(int cycle) {
        return cycleToGeneratedData.getOrDefault(cycle, null);
    }

    public double getNextTimeExecution(double clock) {
        return executionModel.nextExecutionTime(clock);
    }

    public long getFileSize(String fileName) {
        return this.fileMap.get(fileName);
    }

    public List<String> neededFrom(Integer parentId) {
        return this.neededFromParent.get(parentId);
    }

    public boolean isRoot() {
        return getParents().size() == 0;
    }

    public void setRam(Double ram) {
        this.ram = ram;
    }

    public void setBw(Double bw) {
        this.bw = bw;
    }

    @Override
    public double getProcessingCost() {
        double cost = getCostPerSec() * getActualCPUTime();

        double fileSize = 0;
        for (Data file : getInputFiles()) {
            fileSize += file.getSize() / Consts.MILLION;
        }
        cost += costPerBw * fileSize;
        return cost;
    }

    public void addFailedExecution(double time) {
        this.failedExecutions.add(time);
    }

    public ArrayList<Double> getFailedExecutions() {
        return failedExecutions;
    }
}
