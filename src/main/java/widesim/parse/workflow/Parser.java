package widesim.parse.workflow;

import widesim.computation.*;
import widesim.core.Enums.UtilizationModelEnum;
import widesim.parse.Default;
import org.cloudbus.cloudsim.UtilizationModel;
import org.jgrapht.alg.util.Pair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static widesim.parse.Helper.getOrDefault;

public class Parser {

    private final String workflowsJsonContent;

    public Parser(File workflowsFile) throws IOException {
        if (!workflowsFile.exists())
            throw new IOException(String.format("File: %s does not exist", workflowsFile.getPath()));

        if (!workflowsFile.isFile())
            throw new IllegalArgumentException(String.format("Path: %s is not a file", workflowsFile.getPath()));

        if (!workflowsFile.canRead())
            throw new IllegalAccessError(String.format("Misty does not have READ access to file: %s", workflowsFile.getPath()));

        List<String> lines = Files.readAllLines(workflowsFile.toPath());

        // Concat all lines
        workflowsJsonContent = String.join("\n", lines);
    }

    public List<Workflow> parse() throws JSONException {

        JSONObject root = new JSONObject(workflowsJsonContent);

        return root.getJSONArray(Tags.WORKFLOWS).toList().stream().map(workflowNode -> {
            JSONObject workflowObj = new JSONObject((Map<?, ?>) workflowNode);
            String workflowId = workflowObj.getString(Tags.WORKFLOW_ID);

            // Map each file to its owner
            Map<String, Integer> fileToOwner = new HashMap<>();

            List<TaskInfo> tasksInfo = workflowObj.getJSONArray(Tags.TASKS).toList().stream().map(task -> {
                JSONObject taskObj = new JSONObject((Map<?, ?>) task);

                int taskId = taskObj.getInt(Tags.TASK_ID);
                long runtime = taskObj.getLong(Tags.RUNTIME);

                // Parse input files
                List<widesim.parse.dax.File> inputFiles = taskObj.getJSONArray(Tags.INPUT_FILES).toList().stream().map(file -> {
                    JSONObject fileObj = new JSONObject((Map<?, ?>) file);
                    String file_id = fileObj.getString(Tags.FILE_ID);
                    long size = fileObj.getLong(Tags.SIZE);

                    return new widesim.parse.dax.File(file_id, size);
                }).collect(Collectors.toList());

                // Parse output files
                List<widesim.parse.dax.File> outputFiles = taskObj.getJSONArray(Tags.OUTPUT_FILES).toList().stream().map(file -> {
                    JSONObject fileObj = new JSONObject((Map<?, ?>) file);
                    String file_id = fileObj.getString(Tags.FILE_ID);
                    long size = fileObj.getLong(Tags.SIZE);

                    return new widesim.parse.dax.File(file_id, size);
                }).collect(Collectors.toList());

                // Map each file to its size
                Map<String, Long> fileMap = new HashMap<>();

                // Map each file to its owner
                outputFiles.forEach(file -> {
                    fileToOwner.put(file.getId(), taskId);
                    fileMap.put(file.getId(), file.getSize());
                });

                double ram = getOrDefault(taskObj, Tags.RAM, 0.0, Double.class);
                double bw = getOrDefault(taskObj, Tags.BW, 0.0, Double.class);

                Integer vmId = getOrDefault(taskObj, Tags.VM_ID, null, Integer.class);

                FractionalSelectivity selectivity = new FractionalSelectivity(getOrDefault(taskObj, Tags.SELECTIVITY, 1.0, Double.class));

                ExecutionModel executionModel = new PeriodicExecutionModel(getOrDefault(taskObj, Tags.EXECUTION_PERIOD, 1.0, Double.class));

                long inputFileSize = inputFiles.stream().map(widesim.parse.dax.File::getSize).reduce(0L, Long::sum);

                long outputFileSize = outputFiles.stream().map(widesim.parse.dax.File::getSize).reduce(0L, Long::sum);

                double entry_time = getOrDefault(taskObj, Tags.ENTRY_TIME, Default.TASK.ENTRY_TIME, Double.class);
                double deadLine = getOrDefault(taskObj, Tags.DEAD_LINE, Default.TASK.DEAD_LINE, Double.class);

                String cpuUtilModel = getOrDefault(taskObj, Tags.CPU_UTIL, Default.TASK.CPU_UTIL_MODEL.toString(), String.class);
                String ramUtilModel = getOrDefault(taskObj, Tags.RAM_UTIL, Default.TASK.RAM_UTIL_MODEL.toString(), String.class);
                String bwUtilModel = getOrDefault(taskObj, Tags.BW_UTIL_MODEL, Default.TASK.BW_UTIL_MODEL.toString(), String.class);
                int pes = getOrDefault(taskObj, Tags.NUM_OF_PES, Default.TASK.NUM_OF_PES, Integer.class);

                return new TaskInfo(
                        taskId, runtime, pes, inputFileSize, outputFileSize,
                        UtilizationModelEnum.getUtilizationModel(cpuUtilModel),
                        UtilizationModelEnum.getUtilizationModel(ramUtilModel),
                        UtilizationModelEnum.getUtilizationModel(bwUtilModel),
                        inputFiles, outputFiles, deadLine, entry_time, workflowId,
                        selectivity, executionModel, ram, bw, vmId, fileMap
                );
            }).collect(Collectors.toList());

            List<Task> tasks = new ArrayList<>();
            for (TaskInfo taskInfo: tasksInfo) {
                List<Pair<Integer, String>> neededFilesPair = taskInfo.inputFiles.stream().map(f -> Pair.of(fileToOwner.getOrDefault(f.getId(), taskInfo.taskId), f.getId())).collect(Collectors.toList());
                Map<Integer, List<String>> neededFiles = new HashMap<>();
                neededFilesPair.forEach(pair -> {
                    neededFiles.computeIfAbsent(pair.getFirst(), k -> new ArrayList<>());

                    neededFiles.get(pair.getFirst()).add(pair.getSecond());
                });

                List<Data> inputFiles = taskInfo.inputFiles.stream().map(f -> new Data(f.getId(), fileToOwner.getOrDefault(f.getId(), taskInfo.taskId), taskInfo.taskId, f.getSize())).collect(Collectors.toList());
                Task task = new Task(
                        taskInfo.taskId, taskInfo.runtime, taskInfo.pes, taskInfo.inputFileSize, taskInfo.outputFileSize,
                        taskInfo.cpuModel, taskInfo.ramModel, taskInfo.bwModel, inputFiles,
                        taskInfo.deadline, taskInfo.entryTime, taskInfo.workflowId, taskInfo.selectivityModel,
                        taskInfo.executionModel, taskInfo.ram, taskInfo.bw, taskInfo.vmId
                );

                task.setFileMap(taskInfo.fileMap);
                task.setNeededFromParent(neededFiles);

                tasks.add(task);
            }

            Workflow workflow = new Workflow(tasks, workflowId);

            PostProcessor.connectParentTasksToChildren(workflow);

            PostProcessor.connectChildTasksToParent(workflow);

            return workflow;
        }).collect(Collectors.toList());
    }

    private static class TaskInfo {
        public int taskId;
        public long runtime;
        public int pes;
        public long inputFileSize;
        public long outputFileSize;
        public UtilizationModel cpuModel;
        public UtilizationModel ramModel;
        public UtilizationModel bwModel;
        public List<widesim.parse.dax.File> inputFiles;
        public List<widesim.parse.dax.File> outputFiles;
        public double deadline;
        public double entryTime;
        public String workflowId;
        public SelectivityModel selectivityModel;
        public ExecutionModel executionModel;
        public Double ram;
        public Double bw;
        public Integer vmId;
        public Map<String, Long> fileMap;

        public TaskInfo(int taskId, long runtime, int pes, long inputFileSize, long outputFileSize, UtilizationModel cpuModel,
                        UtilizationModel ramModel, UtilizationModel bwModel, List<widesim.parse.dax.File> inputFiles,
                        List<widesim.parse.dax.File> outputFiles, double deadline, double entryTime, String workflowId,
                        SelectivityModel selectivityModel, ExecutionModel executionModel, Double ram, Double bw, Integer vmId, Map<String, Long> fileMap) {
            this.taskId = taskId;
            this.runtime = runtime;
            this.pes = pes;
            this.inputFileSize = inputFileSize;
            this.outputFileSize = outputFileSize;
            this.cpuModel = cpuModel;
            this.ramModel = ramModel;
            this.bwModel = bwModel;
            this.inputFiles = inputFiles;
            this.outputFiles = outputFiles;
            this.deadline = deadline;
            this.entryTime = entryTime;
            this.workflowId = workflowId;
            this.selectivityModel = selectivityModel;
            this.executionModel = executionModel;
            this.ram = ram;
            this.bw = bw;
            this.vmId = vmId;
            this.fileMap = fileMap;
        }


    }

    private static class Tags {
        public static final String WORKFLOWS = "workflows";
        public static final String WORKFLOW_ID = "workflow_id";
        public static final String TASKS = "tasks";
        public static final String TASK_ID = "task_id";
        public static final String RUNTIME = "runtime";
        public static final String INPUT_FILES = "input_files";
        public static final String OUTPUT_FILES = "output_files";
        public static final String FROM_TASK = "from_task";
        public static final String SIZE = "size";
        public static final String OUTPUT_FILE_SIZE = "output_file_size";
        public static final String CHILDREN = "children";
        public static final String DEAD_LINE = "dead_line";
        public static final String ENTRY_TIME = "entry_time";
        public static final String CPU_UTIL = "cpu_util_model";
        public static final String RAM_UTIL = "ram_util_model";
        public static final String BW_UTIL_MODEL = "bw_util_model";
        public static final String NUM_OF_PES = "num_of_pes";
        public static final String SELECTIVITY = "selectivity";
        public static final String EXECUTION_PERIOD = "execution_period";
        public static final String RAM = "ram";
        public static final String BW = "bw";
        public static final String VM_ID = "vm_id";
        public static final String FILE_ID = "file_id";
    }
}
