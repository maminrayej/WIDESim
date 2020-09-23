package misty.analyze;

import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class WorkflowAnalyzer {

    private final Graph<Integer, DefaultEdge> workflow;

    public WorkflowAnalyzer() {
        workflow = new DefaultDirectedGraph<>(DefaultEdge.class);
    }

    public void addVertex(int taskId) {
        workflow.addVertex(taskId);
    }

    public void addEdge(int srcTaskId, int dstTaskId) {
        workflow.addEdge(srcTaskId, dstTaskId);
    }

    public boolean hasCycle() {
        return new CycleDetector<>(workflow).detectCycles();
    }
}
