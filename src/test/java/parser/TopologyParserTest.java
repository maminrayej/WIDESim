package parser;

import misty.parse.topology.Parser;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

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
}
