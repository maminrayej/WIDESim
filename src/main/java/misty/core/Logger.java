package misty.core;

import com.jakewharton.fliptables.FlipTable;
import misty.computation.Task;
import misty.computation.TaskState.State;
import misty.entity.FogDevice;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class Logger {
    public static void log(String tag, String formatted, Object... args) {
        System.out.printf("[%.2f]|[%s]: ", CloudSim.clock(), tag);

        System.out.printf(formatted, args);

        System.out.println();
    }

    public static void printResult(int cycle, List<Task> tasks, Map<Integer, Integer> vmToFogDevice, List<FogDevice> fogDevices) {
        String[] headers = {
                "Task ID",
//                "On Device",
                "On Vm",
//                "Enter Broker Waiting Queue",
//                "Exit Broker Waiting Queue",
//                "Enter FogDevice Waiting Queue",
//                "Exit Fog Device Waiting Queue",
                "Start Execution Time",
                "End Execution Time",
                "Duration",
        };
        String[][] data = new String[tasks.size()][6];

        IntStream.range(0, tasks.size()).forEach(index -> {
            Task task = tasks.get(index);
            State state = task.getTaskState().getState(cycle);

//            String fogDeviceName = "";
//            for (FogDevice fogDevice: fogDevices) {
//                if (fogDevice.getId() == vmToFogDevice.get(task.getVmId())) {
//                    fogDeviceName = fogDevice.getName();
//                }
//            }

            data[index] = new String[]{
                    task.getTaskId() + "",
//                    fogDeviceName,
                    task.getVmId() + "",
//                    state.enterBrokerWaitingQueue + "",
//                    state.exitBrokerWaitingQueue + "",
//                    state.enterFogDeviceWaitingQueue + "",
//                    state.exitFogDeviceWaitingQueue + "",
                    String.format("%.2f", state.startExecutionTime),
                    String.format("%.2f", state.endExecutionTime),
                    String.format("%.2f", state.endExecutionTime - state.startExecutionTime),
            };
        });

        System.out.println(FlipTable.of(headers, data));
    }
}
