package auction.shared.dto;

public class ItemDTO {
    // ➕ ĐÃ THÊM: Thuộc tính id để định danh sản phẩm giữa Client và Server
    private int id;
    private String name;
    private String description;
    private double startingPrice;
    private String sellerUsername;
    private String image; // Thuộc tính lưu trữ chuỗi Base64 ảnh từ Client

    public ItemDTO() {}

    // ➕ ĐÃ THÊM: Getter và Setter cho id
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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