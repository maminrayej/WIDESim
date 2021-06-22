package misty.parse.dax;

import java.util.List;
import java.util.Map;

public class Job {
    private final int id;
    private final long runtime;
    private final List<File> inputFiles;
    private final List<File> outputFiles;

    private final Map<String, Long> fileMap;

    public Job(int id, long runtime, List<File> inputFiles, List<File> outputFiles, Map<String, Long> fileMap) {
        this.id = id;
        this.runtime = runtime;
        this.inputFiles = inputFiles;
        this.outputFiles = outputFiles;
        this.fileMap = fileMap;
    }

    public int getId() {
        return id;
    }

    public long getRuntime() {
        return runtime;
    }

    public List<File> getInputFiles() {
        return inputFiles;
    }

    public List<File> getOutputFiles() {
        return outputFiles;
    }

    public Map<String, Long> getFileMap() {
        return fileMap;
    }
}
