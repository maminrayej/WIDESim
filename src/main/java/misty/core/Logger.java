package misty.core;

import com.jakewharton.fliptables.FlipTable;
import misty.computation.Task;
import misty.computation.TaskState.State;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.List;
import java.util.stream.IntStream;

public class Logger {
    public static void log(String tag, String formatted, Object... args) {
        System.out.printf("[%.2f]|[%s]: ", CloudSim.clock(), tag);

        System.out.printf(formatted, args);

        System.out.println();
    }

    public static void printResult(int cycle, List<Task> tasks) {
        String[] headers = {
                "Task ID",
                "Enter Broker Waiting Queue",
                "Exit Broker Waiting Queue",
                "Enter FogDevice Waiting Queue",
                "Exit Fog Device Waiting Queue",
                "Start Execution Time",
                "End Execution Time"
        };
        String[][] data = new String[tasks.size()][6];

        IntStream.range(0, tasks.size()).forEach(index -> {
            Task task = tasks.get(index);
            State state = task.getTaskState().getState(cycle);

            data[index] = new String[]{
                    task.getTaskId() + "",
                    state.enterBrokerWaitingQueue + "",
                    state.exitBrokerWaitingQueue + "",
                    state.enterFogDeviceWaitingQueue + "",
                    state.exitFogDeviceWaitingQueue + "",
                    state.startExecutionTime + "",
                    state.endExecutionTime + ""
            };
        });

        System.out.println(FlipTable.of(headers, data));
    }
}
