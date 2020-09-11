package parse.topology;

import analyze.TopologyAnalyzer;
import entity.FogDevice;
import entity.FogHost;
import org.jgrapht.alg.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

public class TopologyProcessor {

    private final String topologyJsonContent;

    public TopologyProcessor(File topologyFile) throws IOException {

        if (!topologyFile.exists())
            throw new IOException(String.format("File: %s does not exist", topologyFile.getPath()));

        if (!topologyFile.isFile())
            throw new IllegalArgumentException(String.format("Path: %s is not a file", topologyFile.getPath()));

        if (!topologyFile.canRead())
            throw new IllegalAccessError(String.format("Misty does not have READ access to file: %s", topologyFile.getPath()));

        // Read content of topology.json file
        List<String> lines = Files.readAllLines(topologyFile.toPath());

        // Concat all lines
        topologyJsonContent = String.join("\n", lines);
    }

    public List<FogDevice> process() {
        // Parse the content and get fog devices
        List<FogDevice> fogDevices = TopologyParser.parse(topologyJsonContent);

        // Connect each host to its fog device
        for (FogDevice fogDevice : fogDevices)
            for (FogHost host : fogDevice.getHosts())
                host.setDatacenter(fogDevice);

        // Create topology graph
        TopologyAnalyzer analyzer = new TopologyAnalyzer();
        for (FogDevice fogDevice : fogDevices) {
            for (String neighbor : fogDevice.getNeighbors()) {
                analyzer.addVertex(fogDevice.getName());
                analyzer.addVertex(neighbor);
                analyzer.addEdge(fogDevice.getName(), neighbor);
            }
        }

        // Update route table of each fog device
        HashMap<Pair<String, String>, String> routingTable = analyzer.getRoutingTable();
        for (FogDevice fogDevice : fogDevices) {
            for (String neighbor : fogDevice.getNeighbors()) {
                String hop = routingTable.getOrDefault(Pair.of(fogDevice.getName(), neighbor), null);
                fogDevice.addRoute(neighbor, hop);
            }
        }

        return fogDevices;
    }
}
