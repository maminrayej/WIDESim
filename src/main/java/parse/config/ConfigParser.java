package parse.config;

import org.json.JSONException;
import org.json.JSONObject;

import static parse.Helper.getOrDefault;

public class ConfigParser {

    public static void parse(String json) throws JSONException  {
        JSONObject root = new JSONObject(json);

        if (!root.isEmpty()) {
            // Parse configs related to scheduler
            if (!root.isNull("scheduler")) {
                JSONObject schedulerConfigObj = root.getJSONObject("scheduler");
                String algorithm = getOrDefault(schedulerConfigObj, "algorithm", "Simple", String.class);
            }
        }
    }
}
