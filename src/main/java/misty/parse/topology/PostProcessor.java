package misty.parse.topology;

import misty.analyze.TopologyAnalyzer;
import misty.entity.FogDevice;
import misty.entity.FogHost;
import org.jgrapht.alg.util.Pair;

import java.util.HashMap;
import java.util.List;

public class PostProcessor {

    public static void connectHostToDatabase(List<FogDevice> fogDevices) {
        // Connect each host to its fog device
        for (FogDevice fogDevice : fogDevices)
            for (FogHost host : fogDevice.getHosts())
                host.setDatacenter(fogDevice);
    }

    public static TopologyAnalyzer buildTopologyAnalyzer(List<FogDevice> fogDevices) {
        // Create topology graph
        TopologyAnalyzer analyzer = new TopologyAnalyzer();
        for (FogDevice fogDevice : fogDevices) {
            for (String neighbor : fogDevice.getNeighbors()) {
                analyzer.addVertex(fogDevice.getName());
                analyzer.addVertex(neighbor);
                analyzer.addEdge(fogDevice.getName(), neighbor);
            }
        }

        return analyzer;
    }

    public static void setRoutingTableOfFogDevices(List<FogDevice> fogDevices,
                                                   HashMap<Pair<String, String>, String> routingTable) {
        for (FogDevice fogDevice : fogDevices) {
            for (String neighbor : fogDevice.getNeighbors()) {
                String hop = routingTable.getOrDefault(Pair.of(fogDevice.getName(), neighbor), null);
                fogDevice.addRoute(neighbor, hop);
            }
        }
    }
}
