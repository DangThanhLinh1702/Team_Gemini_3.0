package auction.server.model;

import auction.server.repository.ItemRepository;

import java.util.UUID;

public class Item extends Entity{
    private int id;
    private String name;
    private String description;
    private double startingPrice;
    private String sellerUserName;
    private int sellerId;
    private final ItemRepository itemRepository = new ItemRepository();


    public Item(String name, String description, double startingPrice, String sellerUserName) {
        this.id = id; // Nhận ID từ database truyền vào
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerUserName = sellerUserName;
    }

    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public int getSellerId() { return sellerId; }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getSellerUserName() {
        return sellerUserName;
    }
    public Item findItemById(int id) {
        return itemRepository.findById(id);
    }

}
