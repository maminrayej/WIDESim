package misty.analyze;

import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class WorkflowAnalyzer {

    private final Graph<String, DefaultEdge> workflow;

    public WorkflowAnalyzer() {
        workflow = new DefaultDirectedGraph<>(DefaultEdge.class);
    }

    public void addVertex(String taskId) {
        workflow.addVertex(taskId);
    }

    public void addEdge(String srcTaskId, String dstTaskId) {
        workflow.addEdge(srcTaskId, dstTaskId);
    }

    public boolean hasCycle() {
        return new CycleDetector<>(workflow).detectCycles();
    }
}
