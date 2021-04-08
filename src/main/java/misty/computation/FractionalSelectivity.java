package misty.computation;

import java.util.Random;

public class FractionalSelectivity implements SelectivityModel {

    private final double fraction;

    public FractionalSelectivity(double fraction) {
        this.fraction = fraction;
    }

    @Override
    public boolean generateData(double clock) {
        return new Random().nextDouble() <= fraction;
    }
}
