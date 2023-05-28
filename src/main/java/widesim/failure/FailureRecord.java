package widesim.failure;


public class FailureRecord {

    public double length;

    public int failedTasksNum;

    public int depth;//used as type

    public int allTaskNum;

    public int vmId;

    public int jobId;

    public int workflowId;

    public double delayLength;

    public FailureRecord(double length, int tasks, int depth, int all, int vm, int job, int workflow) {
        this.length = length;
        this.failedTasksNum = tasks;
        this.depth = depth;
        this.allTaskNum = all;
        this.vmId = vm;
        this.jobId = job;
        this.workflowId = workflow;
    }
}
