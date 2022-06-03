package widesim.parse.dax;

import widesim.computation.*;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.jgrapht.alg.util.Pair;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
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

        Map<String, List<Integer>> fileToOwner = new HashMap<>();

        List<Job> jobs = new ArrayList<>();

        for (int i = 0; i < jobNodes.getLength(); i++) {
            Node jobNode = jobNodes.item(i);
            NamedNodeMap attributes = jobNode.getAttributes();
            int id = Integer.parseInt(attributes.getNamedItem("id").getNodeValue().substring(2)) + 1;
//            long runtime = 1000L * (long) Double.parseDouble(attributes.getNamedItem("runtime").getNodeValue());
            long runtime = (long)(1000 * Double.parseDouble(attributes.getNamedItem("runtime").getNodeValue()));

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

                    fileToOwner.computeIfAbsent(fileId, key -> new ArrayList<>());

                    fileToOwner.get(fileId).add(id);
                }
            }

            jobs.add(new Job(id, runtime, inputFiles, outputFiles, fileMap));
        }

        // Parse child-parent relationship part
        Map<Integer, List<Integer>> childToParents = new HashMap<>();
        Map<Integer, List<Integer>> parentToChildren = new HashMap<>();

        NodeList childNodes = workflowDax.getElementsByTagName("child");

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            NamedNodeMap attributes = childNode.getAttributes();
            int childId = Integer.parseInt(attributes.getNamedItem("ref").getNodeValue().substring(2)) + 1;

            NodeList parentNodes = ((Element) childNode).getElementsByTagName("parent");
            for (int j = 0; j < parentNodes.getLength(); j++) {
                Node parentNode = parentNodes.item(j);
                NamedNodeMap parentAttributes = parentNode.getAttributes();
                int parentId = Integer.parseInt(parentAttributes.getNamedItem("ref").getNodeValue().substring(2)) + 1;

                // Connect child to its parent
                childToParents.computeIfAbsent(childId, k -> new ArrayList<>());
                childToParents.get(childId).add(parentId);

                // Connect parent to its child
                parentToChildren.computeIfAbsent(parentId, k -> new ArrayList<>());
                parentToChildren.get(parentId).add(childId);
            }
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
                    job.getInputFiles().stream().flatMap(f -> fileToOwner.getOrDefault(f.getId(), List.of(job.getId())).stream().map(owner -> new Data(f.getId(), owner, job.getId(), f.getSize()))).collect(Collectors.toList()),
                    Double.MAX_VALUE,
                    0,
                    workflowName,
                    new FractionalSelectivity(1),
                    new PeriodicExecutionModel(1),
                    null,
                    null,
                    null
            );

            List<Pair<Integer, String>> neededFilesPair = task.getInputFiles().stream().map(data -> Pair.of(data.getSrcTaskId(), data.getFileName())).collect(Collectors.toList());

            Map<Integer, List<String>> neededFiles = new HashMap<>();
            neededFilesPair.forEach(pair -> {
                neededFiles.computeIfAbsent(pair.getFirst(), k -> new ArrayList<>());

                neededFiles.get(pair.getFirst()).add(pair.getSecond());
            });

            task.setParents(new HashSet<>(childToParents.getOrDefault(task.getTaskId(), new ArrayList<>())));
            task.setChildren(parentToChildren.getOrDefault(task.getTaskId(), new ArrayList<>()));

            task.setFileMap(job.getFileMap());
            task.setNeededFromParent(neededFiles);

            tasks.add(task);
        }

        return new Workflow(tasks, workflowName);
    }
}
