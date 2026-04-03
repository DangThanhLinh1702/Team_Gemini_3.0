package auction.shared.dto;

public class ItemDTO {
    private String name;
    private String description;
    private double startingPrice;
    private String sellerUsername;

    public ItemDTO(String name, String description, double startingPrice, String sellerUsername) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerUsername = sellerUsername;
    }

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public double getStartingPrice() {
        return startingPrice;
    }
    public String getSellerUsername() {
        return sellerUsername;
    }
}