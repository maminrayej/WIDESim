package parser;

import misty.computation.Task;
import misty.computation.Workflow;
import misty.core.Enums;
import misty.parse.Default;
import misty.parse.workflow.Parser;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WorkflowParserTest {
    @Nested
    public class FileTest {

        @Test
        @DisplayName("file does not exist")
        void topologyFileDoesNotExist() {
            assertThrows(IOException.class, () -> new Parser(new File("InvalidFile.json")));
        }

        @Test
        @DisplayName("path is a directory")
        void topologyFileIsDir() {
            assertThrows(IllegalArgumentException.class, () -> new Parser(new File("./")));
        }

//        @Test
//        @DisplayName("can not read file")
//        void canNotReadTopologyFile() {
//            assertThrows(IllegalAccessException.class, () -> {
//                new Parser(new File("src/test/resources/parser/can_not_read_file.txt"));
//            });
//        }

        @Test
        @DisplayName("read file successfully")
        void readFileSuccessfully() throws IOException, IllegalAccessException {
            Parser workflowParser = new Parser(new File("src/test/resources/parser/can_read_file.txt"));

            Object content = FieldUtils.readField(workflowParser, "workflowsJsonContent", true);

            assertEquals("test content 1\ntest content 2", content.toString());
        }
    }
    @Nested
    public class WorkflowTest {
        @Test
        @DisplayName("empty workflow")
        void parseEmptyWorkflow() throws IOException {
            Parser parser = new Parser(new File("src/test/resources/parser/workflow/empty.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no workflow id")
        void parseWorkflowWithNoId() throws IOException {
            Parser parser = new Parser(new File("src/test/resources/parser/workflow/no_workflow_id.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no tasks")
        void parseWorkflowWithNoTasks() throws IOException {
            Parser parser = new Parser(new File("src/test/resources/parser/workflow/no_tasks.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no task id")
        void parseWorkflowWithNoTaskId() throws IOException {
            Parser parser = new Parser(new File("src/test/resources/parser/workflow/no_task_id.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("no task runtime")
        void parseWorkflowWithNoTaskRuntime() throws IOException {
            Parser parser = new Parser(new File("src/test/resources/parser/workflow/no_task_runtime.json"));

            assertThrows(JSONException.class, parser::parse);
        }

        @Test
        @DisplayName("parse minimal valid")
        void parseMinimalValidWorkflow() throws IOException {
            Parser parser = new Parser(new File("src/test/resources/parser/workflow/minimal_valid.json"));

            List<Workflow> workflows = parser.parse();

            assertEquals(1, workflows.size());

            Workflow workflow = workflows.get(0);
            assertEquals(1, workflow.getTasks().size());

            Task task = workflow.getTasks().get(0);
            assertEquals(workflow.getWorkflowId(), task.getWorkflowId());
            assertEquals(0, task.getTaskId());
            assertEquals(10, task.getRuntime());
            assertEquals(Default.TASK.ENTRY_TIME, task.getEntryTime());
            assertEquals(Default.TASK.DEAD_LINE, task.getDeadLine());
            assertEquals(Default.TASK.NUM_OF_PES, task.getNumberOfPes());
            assertEquals(Default.TASK.NUM_OF_PES * task.getRuntime(), task.getTotalRuntime());
            assertEquals(Enums.UtilizationModelEnum.getUtilizationModel(Default.TASK.CPU_UTIL_MODEL.toString()).getClass(), task.getUtilizationModelCpu().getClass());
            assertEquals(Enums.UtilizationModelEnum.getUtilizationModel(Default.TASK.RAM_UTIL_MODEL.toString()).getClass(), task.getUtilizationModelRam().getClass());
            assertEquals(Enums.UtilizationModelEnum.getUtilizationModel(Default.TASK.BW_UTIL_MODEL.toString()).getClass(), task.getUtilizationModelBw().getClass());
        }
    }
}
