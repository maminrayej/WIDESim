package misty.parse.workflow;

import misty.computation.Data;
import misty.computation.Task;
import misty.computation.Workflow;
import misty.core.Enums.UtilizationModelEnum;
import misty.parse.Default;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static misty.parse.Helper.getOrDefault;

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

        return root.getJSONArray(Tags.WORKFLOWS).toList().stream().map(workflow -> {
            JSONObject workflowObj = (JSONObject) workflow;
            String workflowId = workflowObj.getString(Tags.WORKFLOW_ID);

            List<Task> tasks = workflowObj.getJSONArray(Tags.TASKS).toList().stream().map(task -> {
                JSONObject taskObj = (JSONObject) task;
                int taskId = taskObj.getInt(Tags.TASK_ID);
                long runtime = taskObj.getLong(Tags.RUNTIME);

                List<Data> inputFiles = taskObj.getJSONArray(Tags.INPUT_FILES).toList().stream().map(file -> {
                    JSONObject fileObj = (JSONObject) file;
                    int srcTaskId = fileObj.getInt(Tags.FROM_TASK);
                    long size = fileObj.getLong(Tags.SIZE);

                    return new Data(srcTaskId, taskId, size);
                }).collect(Collectors.toList());

                long inputFileSize = inputFiles.stream().map(Data::getSize).reduce(0L, Long::sum);

                long outputFileSize = taskObj.getLong(Tags.OUTPUT_FILE_SIZE);

                List<String> childTaskIds = taskObj
                        .getJSONArray(Tags.CHILDREN)
                        .toList()
                        .stream()
                        .map(childId -> ((JSONObject) childId).getString(Tags.TASK_ID))
                        .collect(Collectors.toList());

                double deadLine = getOrDefault(taskObj, Tags.DEAD_LINE, Double.MAX_VALUE, double.class);

                String cpuUtilModel = getOrDefault(taskObj, Tags.CPU_UTIL, Default.TASK.CPU_UTIL_MODEL.toString(), String.class);
                String ramUtilModel = getOrDefault(taskObj, Tags.RAM_UTIL, Default.TASK.RAM_UTIL_MODEL.toString(), String.class);
                String bwUtilModel = getOrDefault(taskObj, Tags.BW_UTIL_MODEL, Default.TASK.BW_UTIL_MODEL.toString(), String.class);
                int pes = getOrDefault(taskObj, Tags.NUM_OF_PES, Default.TASK.NUM_OF_PES, int.class);

                return new Task(
                        taskId, runtime, pes, inputFileSize, outputFileSize,
                        UtilizationModelEnum.getUtilizationModel(cpuUtilModel),
                        UtilizationModelEnum.getUtilizationModel(ramUtilModel),
                        UtilizationModelEnum.getUtilizationModel(bwUtilModel),
                        inputFiles, childTaskIds, deadLine, workflowId
                );
            }).collect(Collectors.toList());

            return new Workflow(tasks);
        }).collect(Collectors.toList());
    }

    private static class Tags {
        public static final String WORKFLOWS = "workflows";
        public static final String WORKFLOW_ID = "workflow_id";
        public static final String TASKS = "tasks";
        public static final String TASK_ID = "task_id";
        public static final String RUNTIME = "runtime";
        public static final String INPUT_FILES = "input_files";
        public static final String FROM_TASK = "from_task";
        public static final String SIZE = "size";
        public static final String OUTPUT_FILE_SIZE = "output_file_size";
        public static final String CHILDREN = "children";
        public static final String DEAD_LINE = "dead_line";
        public static final String CPU_UTIL = "cpu_util_model";
        public static final String RAM_UTIL = "ram_util_model";
        public static final String BW_UTIL_MODEL = "bw_util_model";
        public static final String NUM_OF_PES = "num_of_pes";
    }
}
