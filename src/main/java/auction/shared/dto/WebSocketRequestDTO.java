package auction.shared.dto;

public class WebSocketRequestDTO {
    private String action;
    private String token;
    private String itemId;
    private long price;

    public String getAction() { return action; }
    public String getToken() { return token; }
    public String getItemId() { return itemId; }
    public long getPrice() { return price; }
}