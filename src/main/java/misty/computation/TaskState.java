package misty.computation;

import java.util.HashMap;

public class TaskState {
    private final HashMap<Integer, State> states;

    public TaskState() {
        states = new HashMap<>();
    }

    public void setEnterBrokerWaitingQueue(int cycle, double time) {
        states.computeIfAbsent(cycle, k -> new State());

        states.get(cycle).enterBrokerWaitingQueue = time;
    }

    public void setExitBrokerWaitingQueue(int cycle, double time) {
        states.computeIfAbsent(cycle, k -> new State());

        states.get(cycle).exitBrokerWaitingQueue = time;
    }

    public void setEnterFogDeviceWaitingQueue(int cycle, double time) {
        states.computeIfAbsent(cycle, k -> new State());

        states.get(cycle).enterFogDeviceWaitingQueue = time;
    }

    public void setExitFogDeviceWaitingQueue(int cycle, double time) {
        states.computeIfAbsent(cycle, k -> new State());

        states.get(cycle).exitFogDeviceWaitingQueue = time;
    }

    public void setStartExecutionTime(int cycle, double time) {
        states.computeIfAbsent(cycle, k -> new State());

        states.get(cycle).startExecutionTime = time;
    }

    public void setEndExecutionTime(int cycle, double time) {
        states.computeIfAbsent(cycle, k -> new State());

        states.get(cycle).endExecutionTime = time;
    }

    public State getState(int cycle) {
        return this.states.get(cycle);
    }

    public class State {
        public double enterBrokerWaitingQueue;
        public double exitBrokerWaitingQueue;
        public double enterFogDeviceWaitingQueue;
        public double exitFogDeviceWaitingQueue;
        public double startExecutionTime;
        public double endExecutionTime;

        public State(double enterBrokerWaitingQueue, double exitBrokerWaitingQueue, double enterFogDeviceWaitingQueue, double exitFogDeviceWaitingQueue, double startExecutionTime, double endExecutionTime) {
            this.enterBrokerWaitingQueue = enterBrokerWaitingQueue;
            this.exitBrokerWaitingQueue = exitBrokerWaitingQueue;
            this.enterFogDeviceWaitingQueue = enterFogDeviceWaitingQueue;
            this.exitFogDeviceWaitingQueue = exitFogDeviceWaitingQueue;
            this.startExecutionTime = startExecutionTime;
            this.endExecutionTime = endExecutionTime;
        }

        public State() {
            this(0, 0, 0, 0, 0, 0);
        }


    }
}
