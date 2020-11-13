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
        public static final int INCOMING_TASK = BASE + 1;
        public static final int INIT = BASE + 2;
        public static final int RESOURCE_REQUEST_RESPONSE = BASE + 3;
        public static final int RESOURCE_REQUEST = BASE + 4;
        public static final int VM_CREATE = BASE + 5;
        public static final int VM_CREATE_ACK = BASE + 6;
        public static final int VM_DESTROY = BASE + 7;
        public static final int VM_DESTROY_ACK = BASE + 8;
        public static final int EXECUTE_TASK = BASE + 9;
        public static final int TASK_IS_DONE = BASE + 10;
        public static final int STAGE_OUT_DATA = BASE + 11;
        public static final int BROADCAST_ID = BASE + 12;
        public static final int FOG_TO_FOG = BASE + 13;
        public static final int DOWNLOADED_FOG_TO_FOG = BASE + 14;
        public static final int EXECUTE_TASK_WITH_DATA = BASE + 15;
    }
}
