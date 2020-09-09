package parse;

import org.json.JSONObject;

public class Helper {
    public static <T> T getOr(JSONObject jsonObject, String key, T defaultValue, Class<T> type) {
        if (jsonObject.isNull(key)) {
            return defaultValue;
        } else {
            Object obj = jsonObject.get(key);
            if (obj.getClass() != type)
                throw new ClassCastException(String.format("Type %s is not the same as %s", type, obj.getClass()));

            return type.cast(obj);
        }
    }
}
