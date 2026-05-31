package auction.shared.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.google.gson.Gson;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO Tests")
public class DTOTest {

    private static final Gson gson = new Gson();

    @Test
    @DisplayName("Should create ResponseDTO with status and message")
    public void testResponseDTOCreation() {
        ResponseDTO response = new ResponseDTO("success", "Test message");

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("Test message", response.getMessage());
    }

    @Test
    @DisplayName("Should create ResponseDTO with data")
    public void testResponseDTOWithData() {
        Object data = new String[]{"item1", "item2"};
        ResponseDTO response = new ResponseDTO("success", "Data loaded", data);

        assertEquals("success", response.getStatus());
        assertEquals("Data loaded", response.getMessage());
        assertNotNull(response.getData());
    }

    @Test
    @DisplayName("Should set and get status")
    public void testSetAndGetStatus() {
        ResponseDTO response = new ResponseDTO("success", "Message");
        response.setStatus("fail");

        assertEquals("fail", response.getStatus());
    }

    @Test
    @DisplayName("Should set and get message")
    public void testSetAndGetMessage() {
        ResponseDTO response = new ResponseDTO("success", "Old message");
        response.setMessage("New message");

        assertEquals("New message", response.getMessage());
    }

    @Test
    @DisplayName("Should set and get data")
    public void testSetAndGetData() {
        ResponseDTO response = new ResponseDTO("success", "Message");
        Object newData = new java.util.HashMap<>();
        response.setData(newData);

        assertEquals(newData, response.getData());
    }

    @Test
    @DisplayName("Should deserialize LoginRequestDTO from JSON")
    public void testLoginRequestDTODeserialization() {
        String json = "{\"username\":\"testuser\",\"password\":\"testpass\"}";
        LoginRequestDTO loginRequest = gson.fromJson(json, LoginRequestDTO.class);

        assertNotNull(loginRequest);
        assertEquals("testuser", loginRequest.getUsername());
        assertEquals("testpass", loginRequest.getPassword());
    }

    @Test
    @DisplayName("Should handle null data in ResponseDTO")
    public void testResponseDTOWithNullData() {
        ResponseDTO response = new ResponseDTO("success", "No data", null);

        assertNull(response.getData());
        assertEquals("success", response.getStatus());
    }

    @Test
    @DisplayName("Should serialize ResponseDTO to JSON")
    public void testResponseDTOSerialization() {
        ResponseDTO response = new ResponseDTO("success", "Test", null);
        String json = gson.toJson(response);

        assertNotNull(json);
        assertTrue(json.contains("success"));
        assertTrue(json.contains("Test"));
    }

    @Test
    @DisplayName("Should round-trip ResponseDTO through JSON")
    public void testResponseDTORoundTrip() {
        ResponseDTO original = new ResponseDTO("error", "Error message", null);
        String json = gson.toJson(original);
        ResponseDTO deserialized = gson.fromJson(json, ResponseDTO.class);

        assertEquals(original.getStatus(), deserialized.getStatus());
        assertEquals(original.getMessage(), deserialized.getMessage());
    }
}

