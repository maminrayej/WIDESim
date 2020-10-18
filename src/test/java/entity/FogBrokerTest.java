package entity;

import misty.computation.Task;
import misty.computation.Workflow;
import misty.entity.FogBroker;
import misty.entity.TaskManager;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.cloudbus.cloudsim.core.CloudSim;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FogBrokerTest {

    @Test
    void testStarting() throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);

        // Tasks: 0 -> 1
        List<Task> tasks = new ArrayList<>(10) {{
            var children = new ArrayList<>(List.of(1));
            add(new Task(0, 1, 1, 1, 1,
                    null, null, null,
                    null, children, 0.0, 3.0, null));

            add(new Task(1, 1, 1, 1, 1,
                    null, null, null,
                    null, null, 0.0, 2.0, null));
        }};

        FogBroker broker = new FogBroker("broker");

        TaskManager taskManager = new TaskManager(broker.getId(), new ArrayList<>(1) {{
            add(new Workflow(tasks, null));
        }});

        CloudSim.startSimulation();
    }
}
