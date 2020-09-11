package parse.workflow;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

public class WorkflowParser {

    public static void parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);

        // TODO: Placeholder for Workflow class
        List<Object> workflows = root.getJSONArray("workflows").toList().stream().map(workflow -> {
            JSONObject workflowObj = (JSONObject) workflow;
            String workflowId = workflowObj.getString("workflow_id");

            // TODO: Placeholder for workflow properties class
            List<Object> properties = workflowObj.getJSONArray("properties").toList().stream().map(property -> {
                return null;
            }).collect(Collectors.toList());

            // TODO: Placeholder for Task class
            List<Object> tasks = workflowObj.getJSONArray("tasks").toList().stream().map(task -> {
                JSONObject taskObj = (JSONObject) task;
                String taskId = taskObj.getString("task_id");
                long runtime = taskObj.getLong("runtime");
                // TODO: Placeholder for input file class
                List<Object> inputFiles = taskObj.getJSONArray("input_files").toList().stream().map(file -> {
                    return null;
                }).collect(Collectors.toList());
                long outputFileSize = taskObj.getLong("output_file_size");

                List<String> childTaskIds = taskObj.getJSONArray("children").toList().stream().map(childId -> {
                    return ((JSONObject) childId).getString("task_id");
                }).collect(Collectors.toList());

                // TODO: Placeholder for task properties class
                List<Object> taskProperties = workflowObj.getJSONArray("properties").toList().stream().map(property -> {
                    return null;
                }).collect(Collectors.toList());

                return new Object();
            }).collect(Collectors.toList());

            return new Object();
        }).collect(Collectors.toList());
    }
}
