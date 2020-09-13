package misty.parse.workflow;


import misty.computation.Data;
import misty.computation.Task;
import misty.computation.Workflow;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

import static misty.parse.Helper.getOrDefault;

public class Parser {

    public static List<Workflow> parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);

        return root.getJSONArray("workflows").toList().stream().map(workflow -> {
            JSONObject workflowObj = (JSONObject) workflow;
            String workflowId = workflowObj.getString("workflow_id");

            List<Task> tasks = workflowObj.getJSONArray("tasks").toList().stream().map(task -> {
                JSONObject taskObj = (JSONObject) task;
                int taskId = taskObj.getInt("task_id");
                long runtime = taskObj.getLong("runtime");

                List<Data> inputFiles = taskObj.getJSONArray("input_files").toList().stream().map(file -> {
                    JSONObject fileObj = (JSONObject)file;
                    int srcTaskId = fileObj.getInt("from_task");
                    long size = fileObj.getLong("size");

                    return new Data(srcTaskId, taskId, size);
                }).collect(Collectors.toList());

                long inputFileSize = inputFiles.stream().map(Data::getSize).reduce(0L, Long::sum);

                long outputFileSize = taskObj.getLong("output_file_size");

                List<String> childTaskIds = taskObj
                        .getJSONArray("children")
                        .toList()
                        .stream()
                        .map(childId -> ((JSONObject) childId).getString("task_id"))
                        .collect(Collectors.toList());

                double deadLine = getOrDefault(taskObj, "dead_line", Double.MAX_VALUE, double.class);

                String cpuUtilModel = getOrDefault(taskObj, "cpu_util", "Full", String.class);
                String ramUtilModel = getOrDefault(taskObj, "ram_util", "Full", String.class);
                String bwUtilModel = getOrDefault(taskObj, "bw_util", "Full", String.class);
                int pes = getOrDefault(taskObj, "num_of_pes", 1, int.class);

                return new Task(
                        taskId, runtime, pes, inputFileSize, outputFileSize,
                        new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull(),
                        inputFiles, childTaskIds, deadLine, workflowId
                );
            }).collect(Collectors.toList());

            return new Workflow(tasks);
        }).collect(Collectors.toList());
    }
}
