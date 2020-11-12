package misty.core;

import org.cloudbus.cloudsim.core.CloudSim;

public class Logger {
    public static void log(String tag, String formatted, Object... args) {
        System.out.printf("[%.2f]|[%s]: ", CloudSim.clock(), tag);

        System.out.printf(formatted, args);

        System.out.println();
    }
}
