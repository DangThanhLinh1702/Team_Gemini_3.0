package auction.shared.dto;

import com.google.gson.annotations.SerializedName;

public class ItemDTO {
    private String name;
    private String description;

    @SerializedName("starting_price")
    private double startingPrice;

    @SerializedName("seller_user_name")
    private String sellerUsername;

    // Constructor chuẩn có đủ 4 tham số
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