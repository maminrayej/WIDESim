package parser;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static misty.parse.Helper.getOrDefault;
import static org.junit.jupiter.api.Assertions.*;

public class HelperTest {

    @Test
    @DisplayName("getOrDefault | String | Present")
    void getOrDefaultString1() {
        JSONObject testObj = new JSONObject("{\"test_key\": \"test_value\"}");

        String value = getOrDefault(testObj, "test_key", "default_value", String.class);

        assertEquals("test_value", value);
    }

    @Test
    @DisplayName("getOrDefault | String | Default")
    void getOrDefaultString2() {
        JSONObject testObj = new JSONObject("{\"test_key\": \"test_value\"}");

        String value = getOrDefault(testObj, "key", "default_value", String.class);

        assertEquals("default_value", value);
    }

    @Test
    @DisplayName("getOrDefault | int | Present")
    void getOrDefaultInt1() {
        JSONObject testObj = new JSONObject("{\"test_key\": 1}");

        int value = getOrDefault(testObj, "test_key", 0, Integer.class);

        assertEquals(1, value);
    }

    @Test
    @DisplayName("getOrDefault | int | Default")
    void getOrDefaultInt2() {
        JSONObject testObj = new JSONObject("{\"test_key\": 1}");

        int value = getOrDefault(testObj, "key", 0, Integer.class);

        assertEquals(0, value);
    }

    @Test
    @DisplayName("getOrDefault | long | Present")
    void getOrDefaultLong1() {
        JSONObject testObj = new JSONObject("{\"test_key\": 1}");

        long value = getOrDefault(testObj, "test_key", 0L, Long.class);

        assertEquals(1, value);
    }

    @Test
    @DisplayName("getOrDefault | long | Default")
    void getOrDefaultLong2() {
        JSONObject testObj = new JSONObject("{\"test_key\": 1}");

        long value = getOrDefault(testObj, "key", 0L, Long.class);

        assertEquals(0, value);
    }

    @Test
    @DisplayName("getOrDefault | double | Present")
    void getOrDefaultDouble1() {
        JSONObject testObj = new JSONObject("{\"test_key\": 1.0}");

        double value = getOrDefault(testObj, "test_key", 0.0, Double.class);

        assertEquals(1, value);
    }

    @Test
    @DisplayName("getOrDefault | double | Default")
    void getOrDefaultDouble2() {
        JSONObject testObj = new JSONObject("{\"test_key\": 1.0}");

        double value = getOrDefault(testObj, "key", 0.0, Double.class);

        assertEquals(0, value);
    }
}
