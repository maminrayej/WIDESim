package misty.analyze;

import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashMap;

public class TopologyAnalyzer {

    Graph<String, DefaultEdge> topology;

    public TopologyAnalyzer() {
        topology = new DefaultDirectedGraph<>(DefaultEdge.class);
    }

    public void addVertex(String vertexId) {
        topology.addVertex(vertexId);
    }

    public void addEdge(String src, String dst) {
        topology.addEdge(src, dst);
    }

    public boolean hasCycle() {
        return new CycleDetector<>(topology).detectCycles();
    }

    public HashMap<Pair<String, String>, String> buildRoutingTable() {
        HashMap<Pair<String, String>, String> routingTable = new HashMap<>();

        FloydWarshallShortestPaths<String, DefaultEdge> algo = new FloydWarshallShortestPaths<>(topology);

        for (String src : topology.vertexSet())
            for (String dst : topology.vertexSet())
                routingTable.put(Pair.of(src, dst), algo.getFirstHop(src, dst));

        return routingTable;
    }

}
