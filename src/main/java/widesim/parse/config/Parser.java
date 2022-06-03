package widesim.parse.config;

import widesim.misc.Config;
import widesim.parse.Default;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static widesim.parse.Helper.getOrDefault;

public class Parser {

    private final String configJsonContent;

    public Parser(File configFile) throws IOException {
        if (!configFile.exists())
            throw new IOException(String.format("File: %s does not exist", configFile.getPath()));

        if (!configFile.isFile())
            throw new IllegalArgumentException(String.format("Path: %s is not a file", configFile.getPath()));

        if (!configFile.canRead())
            throw new IllegalAccessError(String.format("Misty does not have READ access to file: %s", configFile.getPath()));

        List<String> lines = Files.readAllLines(configFile.toPath());

        // Concat all lines
        configJsonContent = String.join("\n", lines);
    }

    public Config parse() throws JSONException {
        JSONObject root = new JSONObject(configJsonContent);

        String topologyFile = Default.CONFIG.TOPOLOGY_FILE;
        String workflowsFile = Default.CONFIG.WORKFLOWS_FILE;

        if (!root.isEmpty()) {
            topologyFile = getOrDefault(root, "topology_file", Default.CONFIG.TOPOLOGY_FILE, String.class);
            workflowsFile = getOrDefault(root, "workflows_file", Default.CONFIG.WORKFLOWS_FILE, String.class);
        }

        return new Config(topologyFile, workflowsFile);
    }
}
