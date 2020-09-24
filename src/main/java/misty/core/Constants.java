package misty.core;

public class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Can not instantiate class: Constants");
    }

    public static final int INVALID_ID = -1;

    public static class DataUnit {
        public static final long BYTE = 8;
        public static final long KB = 1024 * BYTE;
        public static final long MB = 1024 * KB;
        public static final long GB = 1024 * MB;
        public static final long TB = 1024 * GB;
    }

    public static class MetricUnit {
        public static final long DECA = 10;
        public static final long HECTO = 10 * DECA;
        public static final long KILO = 10 * HECTO;
        public static final long MEGA = 10 * KILO;
        public static final long GIGA = 10 * MEGA;
        public static final long TERA = 10 * GIGA;
    }

    public static class PowOfTwo {
        public static final int ONE = 2;
        public static final int TWO = 2 * ONE;
        public static final int THREE = 2 * TWO;
        public static final int FOUR = 2 * THREE;
        public static final int FIVE = 2 * FOUR;
        public static final int SIX = 2 * FIVE;
        public static final int SEVEN = 2 * SIX;
        public static final int EIGHT = 2 * SEVEN;
        public static final int NINE = 2 * EIGHT;
        public static final int TEN = 2 * NINE;
    }

    public static class MsgTag {
        private static final int BASE = 6000;
        public static final int TASK_INCOMING = BASE + 1;
    }
}
