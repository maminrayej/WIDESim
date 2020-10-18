package parser;

import misty.analyze.TopologyAnalyzer;
import misty.entity.FogDevice;
import misty.entity.FogHost;
import misty.parse.topology.Parser;
import misty.parse.topology.PostProcessor;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TopologyPostProcessTest {

    @Test
    @DisplayName("assert host to fog device connection")
    void assertHostToFogDeviceConnection() throws IOException, IllegalAccessException {
        CloudSim.init(1, Calendar.getInstance(), false);

        Parser parser = new Parser(new File("src/test/resources/parser/topology/postprocess/topology.json"));

        Pair<List<FogDevice>, List<Vm>> devicesAndVms = parser.parse();

        List<FogDevice> fogDevices = devicesAndVms.getFirst();

        PostProcessor.connectHostToDatabase(fogDevices);

        for (FogDevice fogDevice : fogDevices)
            for (FogHost fogHost : fogDevice.getHosts())
                assertEquals(fogDevice.getName(), fogHost.getDatacenter().getName());
    }

    @Test
    @DisplayName("check routing table")
    void checkRoutingTable() throws IOException, IllegalAccessException {
        CloudSim.init(1, Calendar.getInstance(), false);

        Parser parser = new Parser(new File("src/test/resources/parser/topology/postprocess/topology.json"));

        Pair<List<FogDevice>, List<Vm>> devicesAndVms = parser.parse();

        List<FogDevice> fogDevices = devicesAndVms.getFirst();

        PostProcessor.connectHostToDatabase(fogDevices);

        TopologyAnalyzer analyzer = PostProcessor.buildTopologyAnalyzer(fogDevices);

        var routingTable = analyzer.buildRoutingTable();

        HashMap<Pair<String,String>, String> expectedRoutingTable = new HashMap<>() {{
            put(Pair.of("device_a", "device_b"), "device_b");
            put(Pair.of("device_a", "device_c"), "device_c");
            put(Pair.of("device_a", "device_d"), "device_c");

            put(Pair.of("device_b", "device_c"), "device_c");
            put(Pair.of("device_b", "device_d"), "device_c");

            put(Pair.of("device_c", "device_d"), "device_d");
        }};

        for (FogDevice src: fogDevices)
            for (FogDevice dst : fogDevices){
                var key = Pair.of(src.getName(), dst.getName());
                assertEquals(expectedRoutingTable.get(key), routingTable.get(key));
            }
    }

    @Test
    @DisplayName("check setting the routing table")
    void checkSettingTheRoutingTable() throws IOException, IllegalAccessException {
        CloudSim.init(1, Calendar.getInstance(), false);

        Parser parser = new Parser(new File("src/test/resources/parser/topology/postprocess/topology.json"));

        Pair<List<FogDevice>, List<Vm>> devicesAndVms = parser.parse();

        List<FogDevice> fogDevices = devicesAndVms.getFirst();

        PostProcessor.connectHostToDatabase(fogDevices);

        TopologyAnalyzer analyzer = PostProcessor.buildTopologyAnalyzer(fogDevices);

        PostProcessor.setRoutingTableOfFogDevices(fogDevices, analyzer.buildRoutingTable());

        HashMap<Pair<String,String>, String> expectedRoutingTable = new HashMap<>() {{
            put(Pair.of("device_a", "device_b"), "device_b");
            put(Pair.of("device_a", "device_c"), "device_c");
            put(Pair.of("device_a", "device_d"), "device_c");

            put(Pair.of("device_b", "device_c"), "device_c");
            put(Pair.of("device_b", "device_d"), "device_c");

            put(Pair.of("device_c", "device_d"), "device_d");
        }};

        for (FogDevice src: fogDevices)
            for (FogDevice dst : fogDevices){
                var key = Pair.of(src.getName(), dst.getName());
                assertEquals(expectedRoutingTable.get(key), src.nextHop(dst.getName()));
            }
    }
}
