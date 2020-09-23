package misty.parse.workflow;

import misty.analyze.WorkflowAnalyzer;
import misty.computation.Task;
import misty.computation.Workflow;

import java.util.HashSet;

public class PostProcessor {

    public static void connectChildTasksToParent(Workflow workflow) {
        for (Task parentTask : workflow.getTasks())
            for (int childId : parentTask.getChildren()) {
                Task childTask = workflow.getTask(childId);
                childTask.addParentId(parentTask.getTaskId());
            }
    }

    public static WorkflowAnalyzer buildWorkflowAnalyzer(Workflow workflow) {
        WorkflowAnalyzer analyzer = new WorkflowAnalyzer();

        for (Task task : workflow.getTasks())
            analyzer.addVertex(task.getTaskId());

        for (Task task : workflow.getTasks())
            for (int childId : task.getChildren())
                analyzer.addEdge(task.getTaskId(), childId);

        return analyzer;
    }

    public static boolean isWorkflowValid(WorkflowAnalyzer analyzer) {
//        HashSet<Integer> taskIds = new HashSet<>();
//
//        for (Task task : workflow.getTasks())
//            taskIds.add(task.getTaskId());
//
//        for (Task task : workflow.getTasks())
//            for (int childId : task.getChildren())
//                if (!taskIds.contains(childId))
//                    return false;

        return !analyzer.hasCycle();
    }
}
