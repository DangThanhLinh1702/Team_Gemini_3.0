package auction.shared.dto;

public class WebSocketRequestDTO {
    private String action;
    private String token;
    private String itemId;
    private String name;
    private String description;
    private double price;
    private int duration; // Thêm mới
    private String image;    // Thêm mới (Base64)

    // Getters and Setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}