package misty.parse.topology;

import misty.analyze.TopologyAnalyzer;
import misty.entity.FogDevice;
import misty.entity.FogHost;
import org.jgrapht.alg.util.Pair;

import java.util.HashMap;
import java.util.List;

public class PostProcessor {

    public static void connectHostToDatacenter(List<FogDevice> fogDevices) {
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
        for (FogDevice src : fogDevices) {
            for (FogDevice dst : fogDevices) {
                String hop = routingTable.getOrDefault(Pair.of(src.getName(), dst.getName()), null);
                src.addRoute(dst.getName(), hop);
            }
        }
    }

    public static HashMap<Pair<Integer, Integer>, Integer> convertNameToId(List<FogDevice> fogDevices,
                                                                           HashMap<Pair<String, String>, String> routingTable) {
        HashMap<Pair<Integer, Integer>, Integer> convertedRoutingTable = new HashMap<>();

//        System.out.println(fogDevices);

        for (Pair<String, String> srcDst: routingTable.keySet()) {
            String srcName = srcDst.getFirst();
            String dstName = srcDst.getSecond();
            String nextHopName = routingTable.get(srcDst);
//            System.out.printf("src: %s - dst: %s - nxt: %s\n", srcName, dstName, nextHopName);

            Integer srcId = IdOfName(srcName, fogDevices);
            Integer dstId = IdOfName(dstName, fogDevices);
            Integer nextHopId = IdOfName(nextHopName, fogDevices);

            convertedRoutingTable.put(Pair.of(srcId, dstId), nextHopId);
        }

        return convertedRoutingTable;
    }

    private static Integer IdOfName(String name, List<FogDevice> fogDevices) {
        var fd = fogDevices.stream().filter(fogDevice -> fogDevice.getName().equals(name)).findFirst();
        if (fd.isEmpty()) {
            return null;
        } else {
            return fd.get().getId();
        }
    }
}
