package entity;

import misty.computation.Task;
import misty.computation.Workflow;
import misty.entity.TaskManager;
import misty.core.Constants;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class TaskManagerTest {

    @Test
    @DisplayName("task sorting | one workflow")
    void checkTaskSortingWithOneWorkflow() throws IllegalAccessException {
        CloudSim.init(1, Calendar.getInstance(), false);

        List<Task> tasks = new ArrayList<>(10) {{
           add(new Task(0, 1, 1, 1, 1,
                   null, null, null,
                   null, null, 0.0, 3.0, null));

            add(new Task(1, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 2.0, null));

            add(new Task(2, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 1.0, null));

            add(new Task(3, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 0.0, null));
        }};

        TaskManager taskManager = new TaskManager(null, new ArrayList<>(1) {{
            add(new Workflow(tasks, null));
        }});

        List<Task> sortedTasks = (List<Task>) FieldUtils.readDeclaredField(taskManager, "tasks", true);

        IntStream.range(0,4).forEach( num -> assertEquals(num , sortedTasks.get(num).getEntryTime()));
    }

    @Test
    @DisplayName("task sorting | multiple workflow")
    void checkTaskSortingWithMultipleWorkflow() throws IllegalAccessException {
        CloudSim.init(1, Calendar.getInstance(), false);

        List<Task> tasks = new ArrayList<>(10) {{
            add(new Task(0, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 3.0, null));

            add(new Task(1, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 2.0, null));

            add(new Task(2, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 1.0, null));

            add(new Task(3, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 0.0, null));
        }};

        List<Task> tasks2 = new ArrayList<>(10) {{
            add(new Task(0, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 10.0, null));

            add(new Task(1, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 5.0, null));

            add(new Task(2, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 8.0, null));

            add(new Task(3, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 6.0, null));
        }};

        List<Task> tasks3 = new ArrayList<>(10) {{
            add(new Task(0, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 4.0, null));

            add(new Task(1, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 9.0, null));

            add(new Task(2, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 7.0, null));

            add(new Task(3, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 11.0, null));
        }};

        TaskManager taskManager = new TaskManager(null, new ArrayList<>(1) {{
            add(new Workflow(tasks, null));
            add(new Workflow(tasks2, null));
            add(new Workflow(tasks3, null));
        }});

        List<Task> sortedTasks = (List<Task>) FieldUtils.readDeclaredField(taskManager, "tasks", true);

        IntStream.range(0,12).forEach( num -> assertEquals(num , sortedTasks.get(num).getEntryTime()));
    }

    @Test
    @DisplayName("task dispatch")
    void checkTaskDispatch() {
        CloudSim.init(1, Calendar.getInstance(), false);

        List<Task> tasks = new ArrayList<>(10) {{
            add(new Task(0, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 3.0, null));

            add(new Task(1, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 2.0, null));

            add(new Task(2, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 1.0, null));

            add(new Task(3, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 0.0, null));

            add(new Task(4, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 4.0, null));

            add(new Task(5, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 10.0, null));
        }};

        DummyBroker dummyBroker = new DummyBroker("dummy_broker");

        TaskManager taskManager = new TaskManager(dummyBroker.getId(), new ArrayList<>(){{
            add(new Workflow(tasks, null));
        }});

        CloudSim.startSimulation();
    }
}

class DummyBroker extends SimEntity {
    public DummyBroker(String name) {
        super(name);
    }

    @Override
    public void startEntity() {
        System.out.println("Initializing dummy broker...");
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == Constants.MsgTag.INCOMING_TASK) {
            Task task = (Task) ev.getData();
            assert task.getEntryTime() == CloudSim.clock();
        }
    }

    @Override
    public void shutdownEntity() {

    }
}
