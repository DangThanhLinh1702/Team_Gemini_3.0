package auction.shared.dto;

public class WebSocketRequestDTO {
    private String action;
    private String token;
    private String itemId;
    private long price;
    private String name;
    private String description;

    public String getAction() { return action; }
    public String getToken() { return token; }
    public String getItemId() { return itemId; }
    public long getPrice() { return price; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}