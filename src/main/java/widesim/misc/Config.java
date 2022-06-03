package widesim.misc;

public class Config {
    private final String topologyFile;
    private final String workflowsFile;

    public Config(String topologyFile, String workflowsFile) {
        this.topologyFile = topologyFile;
        this.workflowsFile = workflowsFile;
    }

    public String getTopologyFile() {
        return topologyFile;
    }

    public String getWorkflowsFile() {
        return workflowsFile;
    }
}
