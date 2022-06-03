package widesim.computation;

public class PeriodicExecutionModel implements ExecutionModel {
    private double t;

    public PeriodicExecutionModel(double t) {
        this.t = t;
    }

    @Override
    public double nextExecutionTime(double clock) {
        return Math.ceil(clock / t) * t;
    }

    public double getT() {
        return t;
    }
}
