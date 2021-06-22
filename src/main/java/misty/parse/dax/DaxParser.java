package misty.parse.dax;

import misty.computation.*;
import misty.parse.workflow.PostProcessor;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.jgrapht.alg.util.Pair;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DaxParser {
    private final Document workflowDax;
    private final String workflowName;

    public DaxParser(String filePath) throws ParserConfigurationException, IOException, SAXException {
        java.io.File file = new java.io.File(filePath);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        this.workflowDax = db.parse(file);
        this.workflowName = file.getName();
    }

    public Workflow buildWorkflow() {
        NodeList jobNodes = workflowDax.getElementsByTagName("job");

        Map<String, Integer> fileToOwner = new HashMap<>();

        List<Job> jobs = new ArrayList<>();

        for (int i = 0; i < jobNodes.getLength(); i++) {
            Node jobNode = jobNodes.item(i);
            NamedNodeMap attributes = jobNode.getAttributes();
            int id = Integer.parseInt(attributes.getNamedItem("id").getNodeValue().substring(2)) + 1;
            long runtime = 1000L * (long) Double.parseDouble(attributes.getNamedItem("runtime").getNodeValue());
            if (runtime < 100)
                runtime = 100;

            NodeList files = ((Element) jobNode).getElementsByTagName("uses");
            List<File> inputFiles = new ArrayList<>();
            List<File> outputFiles = new ArrayList<>();
            Map<String, Long> fileMap = new HashMap<>();

            for (int j = 0; j < files.getLength(); j++) {
                Node file = files.item(j);
                NamedNodeMap fileAttributes = file.getAttributes();
                String fileId = fileAttributes.getNamedItem("file").getNodeValue();
                String type = fileAttributes.getNamedItem("link").getNodeValue();
                long size = Long.parseLong(fileAttributes.getNamedItem("size").getNodeValue());

                if (type.equals("input")) {
                    inputFiles.add(new File(fileId, size));
                } else {
                    outputFiles.add(new File(fileId, size));
                    fileMap.put(fileId, size);
                    fileToOwner.put(fileId, id);
                }
            }

            jobs.add(new Job(id, runtime, inputFiles, outputFiles, fileMap));
        }

        List<Task> tasks = new ArrayList<>();
        for (Job job : jobs) {
            var task = new Task(
                    job.getId(),
                    job.getRuntime(),
                    1,
                    job.getInputFiles().stream().map(File::getSize).reduce(1L, Long::sum),
                    job.getOutputFiles().stream().map(File::getSize).reduce(1L, Long::sum),
                    new UtilizationModelFull(),
                    new UtilizationModelFull(),
                    new UtilizationModelFull(),
                    job.getInputFiles().stream().map(f -> new Data(fileToOwner.getOrDefault(f.getId(), job.getId()), job.getId(), f.getSize())).collect(Collectors.toList()),
                    Double.MAX_VALUE,
                    0,
                    workflowName,
                    new FractionalSelectivity(1),
                    new PeriodicExecutionModel(1),
                    0,
                    0,
                    null
            );

            List<Pair<Integer, String>> neededFilesPair = job.getInputFiles().stream().map(f -> Pair.of(fileToOwner.getOrDefault(f.getId(), job.getId()), f.getId())).collect(Collectors.toList());
            Map<Integer, List<String>> neededFiles = new HashMap<>();
            neededFilesPair.forEach(pair -> {
                neededFiles.computeIfAbsent(pair.getFirst(), k -> new ArrayList<>());

                neededFiles.get(pair.getFirst()).add(pair.getSecond());
            });

            task.setFileMap(job.getFileMap());
            task.setNeededFromParent(neededFiles);

            tasks.add(task);
        }

        Workflow workflow = new Workflow(tasks, workflowName);

        PostProcessor.connectParentTasksToChildren(workflow);

        PostProcessor.connectChildTasksToParent(workflow);

        return workflow;
    }
}
