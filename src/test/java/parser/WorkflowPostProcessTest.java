package parser;

import misty.analyze.WorkflowAnalyzer;
import misty.computation.Task;
import misty.computation.Workflow;
import misty.parse.workflow.Parser;
import misty.parse.workflow.PostProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WorkflowPostProcessTest {

    @Test
    @DisplayName("check child to parent connection")
    void checkChildToParentConnection() throws IOException {
        Parser parser = new Parser(new File("src/test/resources/parser/workflow/postprocess/workflows.json"));

        List<Workflow> workflows = parser.parse();

        Workflow workflow = workflows.get(0);

        PostProcessor.connectChildTasksToParent(workflow);

        Task task0 = workflow.getTask(0);
        assertEquals(0, task0.getParents().size());

        Task task1 = workflow.getTask(1);
        assertEquals(1, task1.getParents().size());
        assertEquals(0, task1.getParents().get(0));

        Task task2 = workflow.getTask(2);
        assertEquals(1, task2.getParents().size());
        assertEquals(0, task2.getParents().get(0));

        Task task3 = workflow.getTask(3);
        assertEquals(2, task3.getParents().size());
        assertTrue(task3.getParents().contains(1));
        assertTrue(task3.getParents().contains(2));
    }

    @Test
    @DisplayName("check workflow validation")
    void checkWorkflowValidation() throws IOException {
        Parser parser = new Parser(new File("src/test/resources/parser/workflow/postprocess/workflows.json"));

        List<Workflow> workflows = parser.parse();

        Workflow workflow = workflows.get(0);

        WorkflowAnalyzer analyzer = PostProcessor.buildWorkflowAnalyzer(workflow);
        assertTrue(PostProcessor.isWorkflowValid(analyzer));
    }

    @Test
    @DisplayName("check cycle detection")
    void checkCycleDetection() throws IOException {
        Parser parser = new Parser(new File("src/test/resources/parser/workflow/postprocess/cycle.json"));

        List<Workflow> workflows = parser.parse();

        Workflow workflow = workflows.get(0);

        WorkflowAnalyzer analyzer = PostProcessor.buildWorkflowAnalyzer(workflow);
        assertFalse(PostProcessor.isWorkflowValid(analyzer));
    }
}
