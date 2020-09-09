package parse;

import org.json.JSONException;
import org.json.JSONObject;

import static parse.Helper.getOr;

public class ConfigParser {

    public static void parse(String json) throws JSONException  {
        JSONObject root = new JSONObject(json);

        if (!root.isEmpty()) {
            // Parse configs related to scheduler
            if (!root.isNull("scheduler")) {
                JSONObject schedulerConfigObj = root.getJSONObject("scheduler");
                String algorithm = getOr(schedulerConfigObj, "algorithm", "Simple", String.class);
            }
        }
    }
}
