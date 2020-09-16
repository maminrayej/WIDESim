package misty.parse.workflow;

import misty.analyze.WorkflowAnalyzer;
import misty.computation.Task;
import misty.computation.Workflow;

import java.util.HashSet;

public class PostProcessor {

    public static WorkflowAnalyzer buildWorkflowAnalyzer(Workflow workflow) {
        WorkflowAnalyzer analyzer = new WorkflowAnalyzer();

        for (Task task : workflow.getTasks())
            analyzer.addVertex(task.getTaskId());

        for (Task task : workflow.getTasks())
            for (String childId : task.getChildren())
                analyzer.addEdge(task.getTaskId(), childId);

        return analyzer;
    }

    public static boolean isWorkflowValid(Workflow workflow) {
        HashSet<String> taskIds = new HashSet<>();

        for (Task task : workflow.getTasks())
            taskIds.add(task.getTaskId());

        for (Task task : workflow.getTasks())
            for (String childId : task.getChildren())
                if (!taskIds.contains(childId))
                    return false;

        return true;
    }
}
