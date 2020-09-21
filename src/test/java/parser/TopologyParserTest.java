package parser;

import misty.entity.FogDevice;
import misty.parse.topology.Parser;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.cloudbus.cloudsim.Vm;
import org.jgrapht.alg.util.Pair;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TopologyParserTest {

    @Nested
    public class FileTest {

        @Test
        @DisplayName("file does not exist")
        void topologyFileDoesNotExist() {
            assertThrows(IOException.class, () -> {
                new Parser(new File("InvalidFile.json"));
            });
        }

        @Test
        @DisplayName("path is a directory")
        void topologyFileIsDir() {
            assertThrows(IllegalArgumentException.class, () -> {
                new Parser(new File("./"));
            });
        }

        @Test
        @DisplayName("can not read file")
        void canNotReadTopologyFile() {
            assertThrows(IllegalAccessException.class, () -> {
                new Parser(new File("src/test/my_res/parser/can_not_read_file.txt"));
            });
        }

        @Test
        @DisplayName("read file successfully")
        void readFileSuccessfully() throws IOException, IllegalAccessException, NoSuchFieldException {
            Parser topologyParser = new Parser(new File("src/test/my_res/parser/can_read_file.txt"));

            Object content = FieldUtils.readField(topologyParser, "topologyJsonContent", true);

            assertEquals("test content 1\ntest content 2", content.toString());
        }
    }

    @Nested
    public class FogDeviceTest {
        @Test
        @DisplayName("empty topology")
        void parseEmptyTopology() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/my_res/parser/topology/empty.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no fog device id")
        void parseFileWithNoDeviceId() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/my_res/parser/topology/no_device_id.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no neighbors array")
        void parseFileWithNoNeighborsList() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/my_res/parser/topology/no_device_id.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no host id")
        void parseFileWithNoHostId() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/my_res/parser/topology/no_host_id.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no vm id")
        void parseFileWithNoVmId() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/my_res/parser/topology/no_vm_id.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no pe id")
        void parseFileWithNoPeId() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/my_res/parser/topology/no_pe_id.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("minimal valid")
        void parseMinimalValidTopology() throws IOException, IllegalAccessException {
            Parser parser = new Parser(new File("src/test/my_res/parser/topology/minimal_valid.json"));

            Pair<List<FogDevice>, List<Vm>> result = parser.parse();
//
//            List<FogDevice> fogDevices = result.getFirst();
//
//            assertEquals(0, fogDevices.stream().filter(fogDevice -> {
//                return false;
//            }).count());
        }
    }
}
