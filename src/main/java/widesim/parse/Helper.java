package widesim.parse;

import org.json.JSONObject;

public class Helper {

    private Helper() {
        throw new UnsupportedOperationException("Can not instantiate class: Helper");
    }

    public static <T> T getOrDefault(JSONObject jsonObject, String key, T defaultValue, Class<T> type) {
        if (jsonObject.isNull(key)) {
            return defaultValue;
        } else {
            String rawValue = jsonObject.get(key).toString();
            try {
                if (Integer.class.equals(type))
                    return (T) Integer.valueOf(rawValue);
                else if (Double.class.equals(type))
                    return (T) Double.valueOf(rawValue);
                else if (Long.class.equals(type))
                    return (T) Long.valueOf(rawValue);
                else if (String.class.equals(type))
                    return (T) rawValue;
                else
                    throw new IllegalArgumentException(String.format("getOrDefault does not support %s", type));
            } catch (NumberFormatException e) {
                throw new ClassCastException(String.format("Can not cast %s to %s", rawValue.getClass(), type));
            }
        }
    }
}
