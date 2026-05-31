package auction.shared.util;

import auction.shared.dto.ResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JSON Utility Tests")
public class JsonUtilTest {

    private static final Gson gson = new Gson();

    @Test
    @DisplayName("Should convert object to JSON string")
    public void testToJson() {
        ResponseDTO response = new ResponseDTO("success", "Test message", null);
        String json = JsonUtil.toJson(response);

        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("success"));
        assertTrue(json.contains("Test message"));
    }

    @Test
    @DisplayName("Should convert complex object to JSON")
    public void testToJsonComplexObject() {
        ResponseDTO response = new ResponseDTO("success", "Data loaded", new String[]{"item1", "item2"});
        String json = JsonUtil.toJson(response);

        assertNotNull(json);
        assertTrue(json.contains("item1"));
        assertTrue(json.contains("item2"));
    }

    @Test
    @DisplayName("Should build standard response JSON")
    public void testBuildResponse() {
        String json = JsonUtil.buildResponse("success", "Operation successful", null);

        assertNotNull(json);
        assertTrue(json.contains("\"status\":\"success\"") || json.contains("\"status\": \"success\""));
        assertTrue(json.contains("Operation successful"));
    }

    @Test
    @DisplayName("Should build response with data")
    public void testBuildResponseWithData() {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");

        String json = JsonUtil.buildResponse("success", "Data attached", data);

        assertNotNull(json);
        assertTrue(json.contains("value1"));
        assertTrue(json.contains("value2"));
    }

    @Test
    @DisplayName("Should handle null data in response")
    public void testBuildResponseWithNullData() {
        String json = JsonUtil.buildResponse("fail", "No data", null);

        assertNotNull(json);
        assertTrue(json.contains("fail"));
        assertTrue(json.contains("No data"));
    }

    @Test
    @DisplayName("Should build error response")
    public void testBuildErrorResponse() {
        String json = JsonUtil.buildResponse("error", "Something went wrong", null);

        assertTrue(json.contains("error"));
        assertTrue(json.contains("Something went wrong"));
    }

    @Test
    @DisplayName("Should parse response JSON back to object")
    public void testParseJsonToObject() {
        String json = JsonUtil.buildResponse("success", "Test", null);
        ResponseDTO response = gson.fromJson(json, ResponseDTO.class);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("Test", response.getMessage());
    }

    @Test
    @DisplayName("Should handle special characters in JSON")
    public void testToJsonWithSpecialCharacters() {
        ResponseDTO response = new ResponseDTO("success", "Message with special chars: @#$%", null);
        String json = JsonUtil.toJson(response);

        assertNotNull(json);
        assertTrue(json.contains("special chars"));
    }
}

