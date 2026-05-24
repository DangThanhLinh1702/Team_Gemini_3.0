package auction.shared.dto;

public class ItemDTO {
    private String name;
    private String description;
    private double startingPrice;
    private String sellerUsername;
    private String image; // Thuộc tính lưu trữ chuỗi Base64 ảnh từ Client

    public ItemDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}